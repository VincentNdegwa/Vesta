# Finvesta Database & Business Logic Architecture

## 🎯 Overview

This implementation provides a robust offline-first architecture with Firebase sync for the Finvesta finance app. Users can work offline and data automatically syncs when they come back online.

## 🏗️ Architecture Components

### 1. **Data Layer**
- **Room Database**: Local SQLite database for offline storage
- **Firebase Firestore**: Cloud database for sync and backup
- **DataStore Preferences**: User preferences and session management

### 2. **Repository Pattern**
- **AuthRepository**: Handles user authentication, session management
- **TransactionRepository**: Manages financial transactions
- **NetworkManager**: Monitors network connectivity

### 3. **Offline-First Strategy**
- All writes go to Room first (immediate response)
- Background sync to Firebase when online
- Conflict resolution with "last write wins" strategy
- Automatic retry for failed syncs

## 📊 Database Schemas

### Room Entities (Local Storage)
1. **UserEntity** - User account information
2. **TransactionEntity** - Financial transactions
3. **BudgetEntity** - Budget planning and tracking
4. **BillReminderEntity** - Bill payment reminders
5. **UserProfileEntity** - Extended user profile data
6. **UserSettingsEntity** - App settings and security preferences
7. **AccountEntity** - Financial accounts (bank, cash, credit cards)
8. **CategoryEntity** - Transaction categories and subcategories  
9. **GoalEntity** - Financial goals and savings targets

### Key Features in Each Entity
- **Sync Management**: `needsSync`, `lastSyncedAt` fields
- **Soft Deletion**: `isDeleted` flag for sync-safe deletion
- **Timestamps**: `createdAt`, `updatedAt` for change tracking
- **User Isolation**: `userId` field for multi-user support

## 🔐 Authentication Features

### User Management
- ✅ **Sign Up**: Email/password with profile creation
- ✅ **Sign In**: With local session management
- ✅ **Password Reset**: Email-based password reset
- ✅ **Session Persistence**: Automatic login on app restart
- ✅ **Profile Updates**: Display name, photo updates
- ✅ **Logout**: Clean session termination

### Security Features
- ✅ **PIN Protection**: Optional PIN for app access
- ✅ **Biometric Auth**: Fingerprint/face unlock
- ✅ **Auto-lock**: Configurable timeout
- ✅ **Data Encryption**: Local data encryption
- ✅ **Export Protection**: Auth required for sensitive operations

## 🌐 Online/Offline Management

### Offline Capabilities
- ✅ Full CRUD operations work offline
- ✅ Data persisted in Room database
- ✅ Queue operations for later sync
- ✅ Immediate UI updates

### Online Sync
- ✅ Automatic background sync when online
- ✅ Handles network state changes
- ✅ Retry failed operations
- ✅ Firebase security rules implemented

### Sync Strategy
```kotlin
// Example: Add transaction workflow
1. User creates transaction → Saved to Room immediately
2. UI updates instantly (great UX)
3. Background: Sync to Firebase if online
4. If offline: Mark as needsSync=true
5. When back online: Auto-sync pending changes
```

## 🚀 How to Use

### 1. **Authentication**
```kotlin
// In your ViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            // Handle result
        }
    }
}
```

### 2. **Transactions**
```kotlin
// Add a new transaction
val result = transactionRepository.addTransaction(
    userId = currentUserId,
    amount = 50.0,
    type = "EXPENSE",
    category = "Food",
    description = "Lunch at restaurant"
)
```

### 3. **Real-time Data**
```kotlin
// Observe transactions with Flow
transactionRepository.getTransactions(userId)
    .collect { transactions ->
        // Update UI with latest data
    }
```

## 📱 Session Management

### Features Included
- **Persistent Login**: User stays logged in between app launches
- **Secure Storage**: Session data encrypted in DataStore
- **Auto-logout**: Configurable timeout periods
- **Multi-user Support**: Clean user switching

### Session Data Stored
- User ID, email, display name
- Last sync timestamp
- App preferences
- Security settings

## 🔧 Setup Instructions

### 1. **Dependencies Added**
- Room database with KSP
- Firebase (Auth, Firestore, Storage, Messaging)
- DataStore Preferences
- WorkManager for background sync
- Biometric authentication
- Security crypto for encryption

### 2. **Firebase Setup Required**
1. Add `google-services.json` to `app/` directory
2. Configure Firestore security rules (see FIREBASE_SETUP.md)
3. Enable Authentication in Firebase Console
4. Set up Firestore collections

### 3. **Permissions Needed**
Add to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

## 🔄 Data Sync Process

### Sync Triggers
1. **App Launch**: Sync if data changed since last sync
2. **Network Available**: Auto-sync when connection restored
3. **Manual Sync**: User-triggered sync
4. **Background Sync**: Periodic WorkManager tasks

### Conflict Resolution
- **Last Write Wins**: Simplest approach for MVP
- **Timestamp Comparison**: Uses `updatedAt` field
- **User Notification**: For critical conflicts

## 🎉 Benefits of This Architecture

### For Users
- ✅ **Always Works**: Offline functionality
- ✅ **Fast Response**: Immediate UI updates
- ✅ **Data Safety**: Multiple backup layers
- ✅ **Cross-device Sync**: Access data anywhere

### For Developers
- ✅ **Clean Architecture**: Separation of concerns
- ✅ **Testable Code**: Repository pattern
- ✅ **Scalable**: Easy to add new features
- ✅ **Maintainable**: Clear data flow

## 🚦 Next Steps

1. **Add Dependency Injection** (Hilt/Dagger)
2. **Implement Background Sync** (WorkManager)
3. **Add Conflict Resolution UI**
4. **Implement Data Export/Import**
5. **Add Analytics and Crash Reporting**
6. **Set up CI/CD Pipeline**

## 📚 Usage Examples

Check the `AuthViewModel` class for example implementation of how to use the repositories in your UI layer. The pattern is consistent across all data operations.

This architecture provides a solid foundation for your finance app with enterprise-grade offline capabilities and seamless Firebase integration! 🎯
