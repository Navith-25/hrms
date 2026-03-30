# DG5 HR Management System (HRMS)

A comprehensive, role-based Human Resource Management System built with Spring Boot, Spring Security, Spring Data JPA, and Thymeleaf. This application streamlines core HR and company operations including employee management, payroll generation, leave tracking, performance reviews, loan processing, task assignment, and internal communications.

## 🌟 Features

* **Role-Based Access Control (RBAC):** Distinct dashboards and permissions for Admin, Director (CEO), HR Manager, HR Staff, Finance Manager, Department Managers, and standard Employees.
* **Employee Management:** Add, update, and remove employee records. Assign departments and managerial roles.
* **Leave Management:** Employees can request leaves; Managers and Admins can approve or reject them.
* **Payroll & Payslips:** Finance and Admins can configure standard deductions, process monthly salaries, add bonuses, and automatically deduct active loans. Employees can view/print their payslips.
* **Loan Management:** Employees can request company loans. Finance managers process approvals, and repayments are automatically deducted from the monthly payroll.
* **Performance Reviews:** Managers and Directors can grade employees on various metrics (Quality of work, communication, etc.) and leave feedback.
* **Task Management:** Managers can assign tasks to their department members, set priorities/deadlines, and review them upon completion.
* **Training Programs:** Directors can create training modules, and managers can assign specific employees to them.
* **Attendance & Calendar:** HR Staff can mark daily attendance. Employees have a personalized calendar view (powered by FullCalendar) showing their attendance, leaves, tasks, and training.
* **Internal Messaging:** Built-in inbox/outbox for employees to send direct messages, or for admins/managers to broadcast department-wide or company-wide notifications.

## 🛠️ Tech Stack

* **Backend:** Java 17, Spring Boot 3.5.x, Spring Security, Spring Data JPA
* **Database:** MySQL
* **Frontend:** Thymeleaf, Bootstrap 5, HTML5, CSS3, FullCalendar.js
* **Build Tool:** Maven

## ⚙️ Prerequisites

* **Java Development Kit (JDK):** Version 17 or higher
* **MySQL Server:** Version 8.0 or higher
* **Maven:** (Or use the included `mvnw` wrapper)

## 🚀 Installation and Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd hrms
