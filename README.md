# 🔍 FindIt – Smart Lost & Found Management System

A modern and secure **Smart Lost & Found Management System** developed using **Java 17, Java Swing, JDBC, and MySQL**, featuring a desktop-based graphical application for reporting lost and found items, searching records, managing ownership claims, viewing item photographs, and identifying potential matches through an intelligent heuristic matching engine.

The project demonstrates Object-Oriented Programming (OOP) concepts in Java, relational database management principles in MySQL, layered application architecture, secure authentication using BCrypt, JDBC transaction management, and role-based authorization.

---

## 🎓 Academic Details

| Course Code | Course Name |
| ----------- | -------------------------------------- |
| **CS23333** | Object Oriented Programming Using Java |
| **CS23332** | Database Management Systems |

- **Project Type:** Academic / College Project
- **Architecture:** Java Swing Desktop Application with JDBC and MySQL
- **Team Size:** 3 Members

---

## 👥 Project Team & Responsibilities

| S. No. | Student Name | Register Number | Role & Focus Area |
| :---: | :--- | :---: | :--- |
| **1** | **ARIVUSELVAN S** | `250701071` | Backend Development, Database Design & JDBC Integration |
| **2** | **ANBARASAN E** | `250701054` | Java Swing UI/UX, User Authentication & Frontend Development |
| **3** | **AKSHATH R** | `250701041` | Claim Management, Smart Matching, Testing & Documentation |

---

## 📌 Project Overview

Managing lost and found belongings in colleges and shared environments can be challenging. Traditional lost-and-found processes often depend on manual announcements, notice boards, and individual communication, making it difficult to identify rightful owners and track reported items.

FindIt provides a centralized desktop platform where users can report lost and found belongings, search existing reports, view item photographs, submit ownership claims, and track report statuses.

The application also provides administrative facilities for reviewing claims, managing reports, handling notifications, and facilitating the identification of potentially matching lost and found items.

The system incorporates three major functional areas:

1. **User Management:** Registration, authentication, profile management, role-based access, and password recovery.
2. **Lost & Found Management:** Item reporting, report searching, filtering, editing, deletion, photograph management, and status tracking.
3. **Intelligent Matching & Claim Processing:** Weighted heuristic matching, claim submission, administrative review, and item return management.

---

## ✨ Key Features

### 🎛️ 1. Central Dashboard & Report Overview
- **Dashboard Statistics:** Displays total reports and report-status counts, including `OPEN`, `CLAIM_PENDING`, `CLAIMED`, and `RETURNED`.
- **Report Navigation:** Dedicated sections for All Reports, Lost Items, Found Items, and My Reports.
- **Interactive KPI Filters:** Clickable dashboard statistic cards filter the report table according to report status.
- **Search & Sorting:** Search reports using keywords and sort table columns.
- **Client-Side Pagination:** Supports page sizes of 10, 25, and 50 reports.
- **Collapsible Sidebar:** Expandable and collapsible application navigation panel.
- **Report Action Toolbar:** Provides access to Smart Matches, View Photo, View/Edit, Delete, Submit Claim, and Mark as Returned operations.
- **Report Preview:** Displays selected report information and associated photographs.
- **Background Loading:** Uses `SwingWorker` for background database operations to keep the interface responsive.

### 👤 2. User Authentication & Profile Management
- Register new user accounts with name, email, and password.
- Authenticate users using BCrypt password hashing.
- Support separate `ADMIN` and `USER` roles.
- **Role-Based Access Control:** Restricts administrative operations to authorized administrators.
- **User Profile Management:** Update account display names.
- **Profile Photograph Management:** Upload, view, change, and remove profile photographs.
- **Authenticated Password Change:** Change account passwords after verifying the existing password.
- **Account Information:** Display registered email, user information, and account details.
- **Session-Based UI Context:** Display user-specific reports and profile information within the dashboard.

### 📦 3. Lost & Found Report Management
- Create new LOST and FOUND reports.
- Enter item names, categories, descriptions, and locations.
- Select item categories dynamically from the MySQL database.
- Attach photographs while creating reports.
- View report details and associated photographs.
- Edit existing reports and update item information.
- Change or remove previously attached photographs.
- **Ownership-Based Authorization:** Allow the original reporter or an administrator to edit or delete reports.
- **Report Deletion:** Remove associated claims and status-history records as part of the report deletion workflow.
- **Return Management:** Mark eligible `CLAIMED` reports as `RETURNED`.
- Maintain report status changes through the `status_history` table.

### 🔎 4. Advanced Report Search & Filtering
- Search reports using item-related keywords.
- Filter reports by LOST and FOUND report types.
- View reports submitted by the currently logged-in user.
- Filter report records using status-based dashboard cards.
- Sort report table columns.
- Navigate large report collections using client-side pagination.
- Display report information including ID, type, item name, category, location, report date, status, and reporter.

### 🤝 5. Ownership Claim Submission & Administration
- Allow users to submit claims for available FOUND reports.
- Validate report type and availability before accepting claims.
- Store claimant details and claim descriptions.
- Change report status to `CLAIM_PENDING` after claim submission.
- Display submitted claims through the administrative claims interface.
- Allow authorized administrators to APPROVE or REJECT pending claims.
- Automatically change an approved report's status to `CLAIMED`.
- Automatically reject other pending claims associated with the same report after approval.
- Prevent repeated review of previously reviewed claims.
- Restore report availability when appropriate after a claim rejection.
- Maintain report status history during claim review operations.

### 🧠 6. Smart Matching Engine
- **Offline Heuristic Matching:** Identifies potential matches between LOST and FOUND reports using a weighted multi-factor similarity algorithm.
- **Category Matching (30 Points):** Compares item category identifiers or category names.
- **Item Name Similarity (30 Points):** Uses Levenshtein distance and token-based similarity to compare item names.
- **Description Keyword Overlap (20 Points):** Uses tokenization and Jaccard similarity to identify common descriptive keywords.
- **Location Similarity (10 Points):** Compares reported locations using string similarity.
- **Date Proximity (10 Points):** Assigns higher scores to reports submitted closer together in time.
- **Match Score:** Calculates an overall similarity score capped at 100.
- **Ranked Results:** Displays potential matching reports in descending order of similarity score.
- **Match Explanations:** Displays contributing factors behind each suggested match.
- **Opposite-Type Matching:** Matches LOST reports against FOUND reports and vice versa.
- **Eligibility Filtering:** Excludes reports marked as `RETURNED` from matching results.
- **Quick Actions:** Allows users to view matching photographs and initiate claims for eligible open FOUND reports.

### 🖼️ 7. Image Storage & Image Viewing
- Browse and attach item photographs while creating reports.
- Change or remove photographs when editing reports.
- Store uploaded images in the application's local `uploads/` directory.
- Generate unique filenames for uploaded images.
- Validate actual image content instead of relying only on filename extensions.
- Enforce a maximum image upload size of **5 MB**.
- Generate rounded item thumbnails for dashboard tables.
- Display full-size item photographs through a dedicated image viewer.
- Generate circular profile-avatar thumbnails.
- Maintain an in-memory thumbnail cache for image rendering.
- Remove associated image files through the image-storage service.

### 📧 8. Email Configuration & Password Recovery
- Configure Gmail SMTP settings using environment variables or an optional local `email.properties` file.
- Send email verification messages through the configured SMTP service.
- Generate cryptographically secure six-digit OTP verification codes.
- Store OTP hashes using SHA-256 instead of storing the verification code directly.
- Enforce a five-minute OTP validity period.
- Apply a 60-second cooldown between OTP requests.
- Track the number of remaining verification attempts.
- Invalidate expired, consumed, or superseded verification tokens.
- Update passwords using BCrypt hashing after successful OTP verification.
- Support OTP resend functionality with a visible countdown.
- Provide a two-step password recovery interface.

### 🔔 9. Notification Management
- Maintain user-specific application notifications.
- Store notification titles, messages, and related report references.
- Retrieve notifications for individual users.
- Display unread notification counts.
- Mark individual notifications as read.
- Mark all notifications as read.
- Provide a centralized notification center within the dashboard.

### 🎨 10. Modern Desktop User Interface
- Modern Java Swing interface with a consistent visual design.
- Light and dark theme support.
- Centralized theme configuration.
- Custom rounded buttons, input components, tables, and dialogs.
- Status badges for report types and report statuses.
- Interactive reporter profile and contact dialog.
- Copy reporter contact information.
- Initiate email communication with reporters.
- Image thumbnails and dedicated image-viewing dialogs.
- Loading indicators and background operations using `SwingWorker`.
- Interactive navigation, table actions, and contextual menus.

---

## 🛠️ Technologies Used

| Technology | Purpose |
| :--- | :--- |
| **Java 17** | Core application development using object-oriented programming concepts |
| **Java Swing** | Desktop graphical user interface and interactive application screens |
| **JDBC** | Direct SQL execution and Java-to-MySQL database connectivity |
| **MySQL 8.x** | Relational database management system |
| **MySQL Connector/J 9.4.0** | JDBC driver for MySQL connectivity |
| **jBCrypt 0.4** | BCrypt password hashing and verification |
| **Jakarta Mail 2.0.1** | Email communication and OTP delivery |
| **Jakarta Activation 2.0.1** | Supporting activation and email content handling |
| **Apache Maven** | Dependency management and build automation |
| **Java AWT & Swing Components** | Desktop layouts, tables, forms, dialogs, and custom graphical components |
| **IntelliJ IDEA** | Application development and debugging |
| **MySQL Workbench** | Database creation, querying, and management |
| **Git & GitHub** | Version control and collaborative project management |

---

## 🧩 Object-Oriented Programming Concepts

| OOP Concept | Implementation in Codebase |
| :--- | :--- |
| **Encapsulation** | Model classes such as `User`, `Item`, `Report`, and `Claim` encapsulate application data through class fields and methods. |
| **Inheritance** | `LostReport` and `FoundReport` extend the common `Report` class and reuse shared report attributes. |
| **Abstraction** | DAO and service layers separate database access and business logic from the presentation layer. |
| **Polymorphism** | The common `Report` reference is used to handle different report types through their shared parent-class structure. |

---

## 🗄️ Database Design

The application uses a relational MySQL database to maintain user information, item details, reports, claims, notification records, and password recovery information.

The major database entities used by the application include:

| Table | Description |
| :--- | :--- |
| `users` | Stores registered user details, email addresses, BCrypt password hashes, roles, registration timestamps, and profile image paths. |
| `items` | Stores item information, category references, descriptions, and image paths. |
| `reports` | Maintains LOST and FOUND report records, reporter references, locations, dates, and report statuses. |
| `claims` | Stores claim submissions, claimant references, claim descriptions, and claim review statuses. |
| `status_history` | Maintains report status transition records, including the user who made the change and previous and new statuses. |
| `notifications` | Stores user-specific notification titles, messages, related report references, and read status. |
| `password_reset_tokens` | Maintains password recovery OTP hashes, attempt counts, expiration timestamps, and token usage status. |

The application uses primary-key and foreign-key relationships to associate users, items, reports, claims, and related records.

---

## 👨‍💻 Team Contributions & Roles

### 1. ARIVUSELVAN S – Backend Development, Database Design & JDBC Integration
- Worked on the core application backend and business logic.
- Developed database access functionality using JDBC and PreparedStatements.
- Worked on MySQL database structure and relational data management.
- Implemented database connectivity through the `DBConnection` utility.
- Contributed to DAO and service-layer integration.
- Worked on maintaining data consistency during critical database operations.

### 2. ANBARASAN E – Java Swing UI/UX, User Authentication & Frontend Development
- Worked on the Java Swing desktop application interface.
- Developed and refined login and registration interfaces.
- Contributed to the dashboard and application navigation.
- Worked on user profile interfaces and authentication workflows.
- Contributed to visual consistency, themes, forms, and interactive components.

### 3. AKSHATH R – Claim Management, Smart Matching, Testing & Documentation
- Worked on claim submission and administrative claim review functionality.
- Contributed to the smart matching module.
- Worked on report and claim workflow validation.
- Performed application testing and feature verification.
- Contributed to technical documentation and project presentation.

*Note: Team responsibilities should reflect each member's actual contribution before final academic submission.*

---

## 🔒 Security, Transaction Management & Data Integrity

### 1. Secure Password Storage
- **BCrypt Password Hashing:** Passwords are stored as BCrypt hashes rather than plain-text values.
- **Password Verification:** Authentication uses BCrypt verification against stored password hashes.
- **Credential Separation:** Database and email credentials are obtained through environment variables or excluded local configuration files.

### 2. SQL Injection Prevention
- Database operations use JDBC `PreparedStatement`.
- User-provided data is passed through parameterized SQL queries.
- This reduces the risk of SQL injection attacks.

### 3. Role-Based Access Control
- The application distinguishes between `ADMIN` and `USER` roles.
- Administrative claim review operations verify the user's role before proceeding.
- Report modification operations verify report ownership or administrator privileges.
- Unauthorized users are prevented from reviewing claims or modifying reports belonging to other users.

### 4. Transaction Management & Claim Concurrency
Critical claim operations use JDBC transactions and pessimistic row-level locking.

1. **Pessimistic Row-Level Locking (`SELECT ... FOR UPDATE`):** Locks the selected report or claim records during critical claim submission and review operations.
2. **Status Validation:** Verifies current claim and report statuses before making changes.
3. **Atomic Updates:** Updates claim and report statuses within a database transaction.
4. **Rollback Handling:** Rolls back the transaction when a database or validation operation fails.

### 5. Claim Integrity & Status Transitions
- Claims can only be submitted against eligible FOUND reports.
- Reports transition to `CLAIM_PENDING` when a claim is successfully submitted.
- Approved claims update the report status to `CLAIMED`.
- Other pending claims for an approved report are automatically rejected.
- Already-reviewed claims cannot be reviewed again.
- Eligible rejected claims can restore the report to `OPEN` when no other pending claims remain.

### 6. OTP Security & Password Recovery
- Verification codes are generated using a secure random-number generator.
- OTP values are stored as SHA-256 hashes.
- Verification codes have a five-minute expiry period.
- A 60-second cooldown restricts repeated OTP requests.
- Failed verification attempts are counted and limited.
- Expired and consumed verification tokens are invalidated.
- Password changes after successful verification are stored using BCrypt.

### 7. Image Validation & Storage
- Actual image content is validated before storage.
- Uploaded images are limited to 5 MB.
- Unique filenames are generated to avoid simple filename collisions.
- Thumbnail caching reduces repeated image processing.

---

## ⚙️ Project Setup & Installation

### Prerequisites
- **Java JDK 17**
- **Apache Maven 3.8+**
- **MySQL Server 8.0+**
- **MySQL Workbench**
- **Git**
- **IntelliJ IDEA**

### 1. Clone the Repository

```bash
git clone https://github.com/arivuselvan42/Java-OOPs-and-DBMS-Project.git
cd Java-OOPs-and-DBMS-Project
```

### 2. Configure MySQL Database

Open MySQL Workbench and connect to your local MySQL Server.

Create the FindIt database:

```sql
CREATE DATABASE IF NOT EXISTS findit_db;
USE findit_db;
```

Create and configure the required tables using the SQL schema corresponding to the current application.

*The repository currently needs a documented database schema script for fully reproducible setup on another computer.*

### 3. Configure Database Credentials

The application reads database credentials through environment variables.

**PowerShell (Windows):**

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/findit_db"
$env:DB_USER = "your_mysql_username"
$env:DB_PASSWORD = "your_mysql_password"
```

Configure these values in IntelliJ under:

`Run → Edit Configurations → Environment variables`

Never commit actual database credentials to GitHub.

### 4. Configure Email Settings

Email functionality uses Gmail SMTP configuration.

The application supports these environment variables:

```powershell
$env:FINDIT_EMAIL = "your_email@gmail.com"
$env:FINDIT_EMAIL_PASSWORD = "your_gmail_app_password"
```

Alternative SMTP variables supported by the configuration are:

- `FINDIT_SMTP_USER`
- `FINDIT_SMTP_PASS`

A local `email.properties` file can also be used. It must remain excluded from version control.

**Important:** Use a Gmail App Password where applicable. Never publish SMTP credentials.

### 5. Build the Project

```bash
mvn clean install
```

This command compiles the Java source code and resolves the Maven dependencies defined in `pom.xml`.

---

## 🚀 Running the Application

### Option A: Using IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA.
2. Select **Open** and choose the cloned FindIt project folder.
3. Wait for Maven dependencies to load.
4. Start your MySQL Server.
5. Configure the required database environment variables.
6. Configure email environment variables if password recovery through email is required.
7. Navigate to:

```text
src/main/java/com/findit/ui/Main.java
```

8. Right-click `Main.java` and select **Run 'Main.main()'**.

The FindIt login screen will appear.

### Option B: Using Maven

Compile the project:

```bash
mvn clean compile
```

Run the application's main class using the IntelliJ execution configuration or an equivalent runtime classpath that includes the required Maven dependencies.

---

## 🚧 Current Scope & Limitations

1. **Desktop Application:** FindIt currently operates as a Java Swing desktop application rather than a web-based or mobile application.
2. **Offline Smart Matching:** The matching engine uses a weighted heuristic algorithm based on category, name, description, location, and date similarity. It does not use an external AI model or machine-learning service.
3. **Local Image Storage:** Item and profile images are stored in the application's local file system. They are not hosted on cloud storage.
4. **Database Dependency:** A running and correctly configured MySQL Server is required for the application's database operations.
5. **Email Dependency:** Email-based password recovery requires valid SMTP configuration.
6. **Database Initialization:** A complete setup guide and distributable SQL schema script should be maintained to make database installation easier for new users.
7. **Matching Accuracy:** Smart matching provides suggestions based on textual and metadata similarity. The suggestions do not independently establish ownership of an item.

---

## 🎯 Key Learning Outcomes

Through this project, the team gained practical hands-on experience in:

- Applying Object-Oriented Programming principles in Java.
- Developing desktop applications using Java Swing.
- Implementing inheritance and shared domain-model structures.
- Connecting Java applications with MySQL through JDBC.
- Designing relational database structures and managing relationships.
- Performing CRUD operations using SQL.
- Using PreparedStatements for safer database access.
- Implementing secure password hashing with BCrypt.
- Developing role-based authentication and authorization.
- Managing multi-step database transactions.
- Applying pessimistic row-level locking using `SELECT ... FOR UPDATE`.
- Implementing an offline weighted similarity-matching algorithm.
- Handling image validation, storage, and thumbnail rendering.
- Implementing email OTP verification and password recovery.
- Using Git and GitHub for version control and collaborative development.

---

## 📜 Academic Purpose

This project was developed in partial fulfillment of the academic requirements for:

- **CS23333 – Object Oriented Programming Using Java**
- **CS23332 – Database Management Systems**

---

**Developed by the FindIt Team**

Department of Computer Science and Engineering  
Rajalakshmi Engineering College

GitHub Repository:  
https://github.com/arivuselvan42/Java-OOPs-and-DBMS-Project