package com.example.tamatoa.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//exporting user data as a csv file
//information found at: https://www.youtube.com/watch?v=BlNHZ2bMwCQ
//and https://chetan-garg36.medium.com/save-your-room-database-to-csv-42db4f742ea1
object CsvExporter {

    private const val TAG = "CsvExporter"
    private const val EXPORT_SUBDIR = "Exports"

    private val displayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    //function to write rows into a csv file
    fun export(context: Context, rows: List<ExpenseWithCategory>): File {
        val dir = File(context.getExternalFilesDir(null), EXPORT_SUBDIR).apply { mkdirs() }
        //name the file with the time stamp it was created
        val filename = "expenses_${filename.format(Date())}.csv"
        val file = File(dir, filename)

        file.bufferedWriter().use { writer ->
            //header row, defining the columns
            writer.appendLine("Date,Category,Amount,Description,Recurring,Has Photo")

            //for every expense row do the following
            for (row in rows) {
                //fetch the date
                val date = displayDate.format(Date(row.dateMillis))
                //fetch if the payment is labeled as recurring (label it as yes/no)
                val recurring = if (row.isRecurring || row.recurringSourceId != null) "Yes" else "No"
                //fetch if there was a photo attached (also a yes/no label)
                val hasPhoto = if (row.photoPath != null) "Yes" else "No"

                //.append(',') after every data field for the comma separating part of csv
                //fetching date, category name, amount of the expense, any attached description
                writer.append(date).append(',')
                writer.append(escape(row.categoryName)).append(',')
                writer.append("%.2f".format(row.amount)).append(',')
                writer.append(escape(row.description)).append(',')
                writer.append(recurring).append(',')
                writer.appendLine(hasPhoto)
            }
        }

        Log.d(TAG, "export: wrote ${rows.size} rows to ${file.absolutePath}")
        return file
    }

    //needed escapes in case the user puts something like: "bread, milk, sugar" in their
    //description of an expense
    private fun escape(value: String): String {
        if (value.isEmpty()) return ""
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuoting) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}