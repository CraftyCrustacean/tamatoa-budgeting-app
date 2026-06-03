# Tamatoa — PROG_3C — Part 2

YouTube video demonstration: <https://youtu.be/-EWjiuehtW4>

## Purpose of the App
Tamatoa is a simple budgeting app that allows users to track their expenses and see where their money goes. It is made to be simple and straight forward for everyday use.
Tamatoa aims to allow users to make insights on their spending by providing easily digestible information in the form of graphs on the dashboard.

## Design Consideration
When designing the app I opted to use a darker colour palette which is easier on users eyes. Each screen has also got its own accent colour to make it easy to tell which screen youre on at a glance. 
Firebase persistence was enabled to allow users to access the app and their locally cached data without an internet connection, any expenses are queued to be sent to the online database when connection is restored.
## Primary Features
### Login and Registration
This screen allows users to create accounts, or log into existing ones. Each user is saved to the database and has their transactions and categories tied to them.

### Dashboard
A place to see summaries of your spending. Including a progress bar showing how much of your budget is used and a section to see total money spent in each category.
Now also features graphs to analyse your spending patterns.

### Add Transaction
Where users can add a new expense specifying the amount, category, date, note, and whether the transaction is recurring. Users can also add pictures to keep track of receipts.

### Transaction list
A place for users to view all expenses over a selected period. Each entry displays the category, date, amount, and marks if an entry has an attached picture or is recurring.

Long pressing an entry allows you to delete that expense.

### Achievement screen
Six earnable badges that can be gained through time based challenges such as "stay within budget for 6 months." Also houses buttons to export data as a csv, load demo data, and to log out.
Tapping on a badge will display its requirements.
### Database
The database has been migrated to firebase, as such the functions to interact with it have changed to reflect firebases required syntax. However, the functionality remains identical to its RoomDB predecessor.

## Own Features
### Feature 1 - Heatmap
A heat map has been included in the dashboard that allows users to visualise what days of the month they spent the most. This allows users to identify patters such as massively increased spending on the weekend.
### Feature 2 - CSV Export
A button to export expenses as a csv is places in the achievements screen. This is to allow users to migrate their data to another app or to run more detailed analytics in software such as r-studio.
## Contributors
- ST10435382 — Zachary Handley
