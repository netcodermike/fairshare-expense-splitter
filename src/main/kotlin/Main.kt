import java.awt.BorderLayout
import java.awt.GridLayout
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.util.Properties
import javax.swing.*

data class Expense(val payer: String, val cents: Long, val note: String)
data class Payment(val from: String, val to: String, val cents: Long)
fun amount(text: String): Long {
    val decimal = text.trim().toBigDecimal().setScale(2, java.math.RoundingMode.UNNECESSARY)
    val cents = decimal.movePointRight(2).longValueExact()
    require(cents in 1..1_000_000_000) { "Amount must be 0.01–10,000,000.00 with at most two decimals" }
    return cents
}
fun money(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
fun balances(people: List<String>, expenses: List<Expense>): Map<String, Long> {
    require(people.isNotEmpty() && people.size <= 100 && people.distinct().size == people.size && people.all { it.isNotBlank() }) { "Use 1–100 unique names" }
    val result = people.associateWith { 0L }.toMutableMap()
    require(expenses.size <= 10000) { "Too many expenses" }
    for (expense in expenses) {
        require(expense.payer in result && expense.cents in 1..1_000_000_000) { "Invalid expense" }
        result[expense.payer] = result.getValue(expense.payer) + expense.cents
        val share = expense.cents / people.size; val extra = expense.cents % people.size
        people.forEachIndexed { i, name -> result[name] = result.getValue(name) - share - if (i.toLong() < extra) 1 else 0 }
    }
    return result
}
fun settle(balance: Map<String, Long>): List<Payment> {
    require(balance.values.sum() == 0L)
    val remaining = balance.toMutableMap(); val payments = mutableListOf<Payment>()
    for (debtor in balance.keys.sorted()) while (remaining.getValue(debtor) < 0) {
        val creditor = remaining.filterValues { it > 0 }.maxByOrNull { it.value }!!.key
        val sum = minOf(-remaining.getValue(debtor), remaining.getValue(creditor))
        payments += Payment(debtor, creditor, sum); remaining[debtor] = remaining.getValue(debtor) + sum; remaining[creditor] = remaining.getValue(creditor) - sum
    }
    return payments
}
fun save(path: Path, people: List<String>, expenses: List<Expense>) {
    balances(people, expenses)
    val p = Properties(); p.setProperty("people.count", people.size.toString()); p.setProperty("expenses.count", expenses.size.toString())
    people.forEachIndexed { i, name -> p.setProperty("person.$i", name) }
    expenses.forEachIndexed { i, e -> p.setProperty("expense.$i.payer", e.payer); p.setProperty("expense.$i.cents", e.cents.toString()); p.setProperty("expense.$i.note", e.note) }
    val full = path.toAbsolutePath(); Files.createDirectories(full.parent); val temp = Files.createTempFile(full.parent, "fairshare-", ".tmp")
    try { Files.newBufferedWriter(temp).use { p.store(it, "FairShare ledger") }; try { Files.move(temp, full, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) } catch (e: AtomicMoveNotSupportedException) { Files.move(temp, full, StandardCopyOption.REPLACE_EXISTING) } }
    finally { Files.deleteIfExists(temp) }
}
fun load(path: Path): Pair<List<String>, List<Expense>> {
    val p = Properties(); Files.newBufferedReader(path).use { p.load(it) }; val n = p.getProperty("people.count").toInt(); val count = p.getProperty("expenses.count").toInt()
    require(n in 1..100 && count in 0..10000)
    val people = (0 until n).map { p.getProperty("person.$it") ?: error("Missing participant") }
    val expenses = (0 until count).map { Expense(p.getProperty("expense.$it.payer"), p.getProperty("expense.$it.cents").toLong(), p.getProperty("expense.$it.note", "")) }
    balances(people, expenses); return people to expenses
}
fun main(args: Array<String>) {
    if (args.contains("--self-test")) {
        val people = listOf("Ada", "Ben", "Cy"); val expenses = listOf(Expense("Ada", 1000, "Dinner"), Expense("Ben", 501, "Lunch")); val balance = balances(people, expenses)
        check(balance.values.sum() == 0L); val rest = balance.toMutableMap(); for (p in settle(balance)) { rest[p.from] = rest.getValue(p.from) + p.cents; rest[p.to] = rest.getValue(p.to) - p.cents }; check(rest.values.all { it == 0L })
        check(amount("12.30") == 1230L); check(runCatching { amount("0.001") }.isFailure)
        val dir = Files.createTempDirectory("fairshare-test"); val file = dir.resolve("ledger.properties"); try { save(file, people, expenses); check(load(file) == people to expenses) } finally { Files.deleteIfExists(file); Files.deleteIfExists(dir) }
        println("Exact cents, remainder distribution, settlements, and persistence passed"); return
    }
    val path = Path.of(args.firstOrNull() ?: "ledger.properties")
    SwingUtilities.invokeLater {
        var people = listOf("Ada", "Ben", "Cy"); var expenses = mutableListOf<Expense>()
        try { if (Files.exists(path)) { val state = load(path); people = state.first; expenses = state.second.toMutableList() } } catch (e: Exception) { JOptionPane.showMessageDialog(null, "Cannot load ledger: ${e.message}"); return@invokeLater }
        val frame = JFrame("FairShare — Expense Splitter").apply { defaultCloseOperation = JFrame.EXIT_ON_CLOSE; setSize(780, 620) }
        val root = JPanel(BorderLayout(12, 12)).apply { border = BorderFactory.createEmptyBorder(20, 20, 20, 20) }; frame.contentPane = root
        val fields = JPanel(GridLayout(0, 2, 8, 8)); val names = JTextField(people.joinToString(", ")); val payer = JTextField(people.first()); val cost = JTextField(); val note = JTextField()
        for ((label, input) in listOf("Participants (comma-separated)" to names, "Who paid?" to payer, "Amount (one currency)" to cost, "Description" to note)) { fields.add(JLabel(label)); fields.add(input) }; root.add(fields, BorderLayout.NORTH)
        val output = JTextArea().apply { isEditable = false; font = java.awt.Font("Monospaced", 0, 15) }; root.add(JScrollPane(output), BorderLayout.CENTER)
        fun draw() { names.isEditable = expenses.isEmpty(); val b = balances(people, expenses); output.text = "EXPENSES\n" + expenses.joinToString("\n") { "${it.payer}: ${money(it.cents)} — ${it.note}" } + "\n\nBALANCES (+ receives, - owes)\n" + b.entries.joinToString("\n") { "${it.key}: ${money(it.value)}" } + "\n\nSETTLEMENT\n" + settle(b).joinToString("\n") { "${it.from} pays ${it.to}: ${money(it.cents)}" } }
        val buttons = JPanel(); root.add(buttons, BorderLayout.SOUTH)
        val add = JButton("Add expense"); buttons.add(add); add.addActionListener {
            try { val nextPeople = names.text.split(',').map { it.trim() }; val e = Expense(payer.text.trim(), amount(cost.text), note.text.trim()); val next = expenses + e; balances(nextPeople, next); save(path, nextPeople, next); people = nextPeople; expenses = next.toMutableList(); cost.text = ""; note.text = ""; draw() }
            catch (e: Exception) { JOptionPane.showMessageDialog(frame, e.message) }
        }
        val undo = JButton("Remove last expense"); buttons.add(undo); undo.addActionListener {
            if (expenses.isNotEmpty()) try { val next = expenses.dropLast(1); save(path, people, next); expenses = next.toMutableList(); draw() } catch (e: Exception) { JOptionPane.showMessageDialog(frame, e.message) }
        }
        val export = JButton("Export settlement text"); buttons.add(export); export.addActionListener {
            val chooser = JFileChooser(); if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) try { chooser.selectedFile.writeText(output.text) } catch (e: Exception) { JOptionPane.showMessageDialog(frame, e.message) }
        }
        draw(); frame.setLocationRelativeTo(null); frame.isVisible = true
    }
}
