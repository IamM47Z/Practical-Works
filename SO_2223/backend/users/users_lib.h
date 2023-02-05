/*
 * users_lib.h
 * 
 * Due to copyright reasons, this header was rewroted and no library was provided!
 * If you wish to run this code you will have to create a library that exports the specified functions.
 */

#ifndef _USERS_H_
#define _USERS_H_

/*
 *  The users file must be formated with the specified format:
 *
 *  <username> <password> <balance>
 *  <username> <password> <balance>
 *  <username> <password> <balance>
 */

// This function is used to read user data from a specified file to be managed internally by the library.
// Returns the number of users read or -1 on error.
//
int loadUsersFile( char* pathname );

// This functions is used to save the user data from the library into a file.
// Returns 0 on success and -1 on error.
//
int saveUsersFile( char* filename );

// This function checks if the given username and password are valid.
// Returns 1 if valid, 0 if the user does not exist or the password is invalid and -1 on error.
//
int isUserValid( char* username, char* password );

// This function returns the balance of the user specified by their username.
// Returns the balance on success or -1 on error.
//
int getUserBalance( char* username );

// This function updates the balance of the user specified by their username.
// Returns 1 on success, 0 if the username is invalid and -1 on error.
//
int updateUserBalance( char* username, int value );

// This function returns a pointer to a buffer containing a descriptive message of the last error.
// The buffer is always rewritten, regardless of success or failure. On success, the buffer will be empty,
// and on failure, it will contain a description of the error.
//
const char * getLastErrorText();

#endif // _USERS_H_