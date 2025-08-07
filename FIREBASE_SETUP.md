# Firebase Configuration Setup

## Required Files

1. **google-services.json**: Download this from your Firebase console and place it in `app/` directory
2. **Firebase Rules**: Set up Firestore security rules

## Firestore Security Rules

Add these rules to your Firebase console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Transactions - users can only access their own
    match /transactions/{transactionId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // Budgets - users can only access their own
    match /budgets/{budgetId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // Bill Reminders - users can only access their own
    match /bill_reminders/{billId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // User Profiles - users can only access their own
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // User Settings - users can only access their own
    match /user_settings/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Accounts - users can only access their own
    match /accounts/{accountId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // Categories - users can only access their own
    match /categories/{categoryId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // Goals - users can only access their own
    match /goals/{goalId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
  }
}
```

## Firebase Collections Structure

### users
- Document ID: user.uid
- Fields: uid, email, displayName, photoUrl, createdAt, updatedAt

### transactions
- Document ID: auto-generated UUID
- Fields: id, userId, amount, type, category, subcategory, description, notes, date, accountId, paymentMethod, location, receiptUrl, tags, recurringId, createdAt, updatedAt, isDeleted

### budgets
- Document ID: auto-generated UUID
- Fields: id, userId, name, category, targetAmount, spentAmount, period, startDate, endDate, alertThreshold, isActive, color, icon, createdAt, updatedAt, isDeleted

### bill_reminders
- Document ID: auto-generated UUID
- Fields: id, userId, title, description, amount, category, dueDate, reminderDays, isRecurring, recurringPeriod, paymentUrl, companyName, accountNumber, isPaid, paidAt, paidAmount, nextDueDate, isActive, createdAt, updatedAt, isDeleted

### user_profiles
- Document ID: user.uid
- Fields: userId, firstName, lastName, phoneNumber, dateOfBirth, address, city, country, currency, language, timezone, profilePicture, occupation, monthlyIncome, createdAt, updatedAt

### user_settings
- Document ID: user.uid
- Fields: userId, isPinEnabled, pinHash, isBiometricEnabled, lockTimeoutMinutes, hideAmounts, requireAuthForExports, requireAuthForReports, darkMode, notificationsEnabled, billRemindersEnabled, budgetAlertsEnabled, backupEnabled, autoBackupFrequency, dataRetentionMonths, createdAt, updatedAt

### accounts
- Document ID: auto-generated UUID
- Fields: id, userId, name, type, balance, currency, bankName, accountNumber, color, icon, isActive, isIncludeInTotal, creditLimit, interestRate, createdAt, updatedAt, isDeleted

### categories
- Document ID: auto-generated UUID
- Fields: id, userId, name, type, parentCategoryId, color, icon, isDefault, isActive, sortOrder, createdAt, updatedAt, isDeleted

### goals
- Document ID: auto-generated UUID
- Fields: id, userId, title, description, targetAmount, currentAmount, targetDate, category, priority, isActive, isCompleted, completedAt, reminderEnabled, reminderFrequency, createdAt, updatedAt, isDeleted
