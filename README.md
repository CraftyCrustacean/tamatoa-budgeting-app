# Tamatoa — PROG_3C — Part 2

YouTube video demonstration: <https://youtu.be/Ioi8NIKOaQ8>

Tamatoa is a simple budgeting app that allows users to track their expenses and see where their money goes.

## Primary features
### Login and Registration
This screen allows users to create accounts, or log into existing ones. Each user is saved to the database and has their transactions and categories tied to them.

### Dashboard
A place to see summaries of your spending. Including a progress bar showing how much of your budget is used and a section to see total money spent in each category.

### Add Transaction
Where users can add a new expense specifying the amount, category, date, note, and whether the transaction is recurring. Users can also add pictures to keep track of receipts.

### Transaction list
A place for users to view all expenses over a selected period. Each entry displays the category, date, amount, and marks if an entry has an attached picture or is recurring.

Long pressing an entry allows you to delete that expense.

### Achievement screen
A placeholder for the next part, but used temporarily as a home for the logout button.

### Database
A database using RoomDB housing 4 tables: Users, Categories, Expenses, and Budget. Each table uses DAOs to handle database operations like date filtering and CRUD operations.

## Contributors
- ST10435382 — Zachary Handley
