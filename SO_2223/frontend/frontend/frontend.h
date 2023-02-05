#ifndef FRONTEND_H
#define FRONTEND_H

#include <pthread.h>
#include <stdbool.h>

#include "../io/io.h"

typedef struct
{
	IO server_io;
	IO client_io;
	
	char username[25];
	char password[25];
	
	char pathname[PATH_MAX];
	
	pthread_t listener_thread;
	pthread_t heartbeat_thread;
	
	bool logged_in;
	unsigned int heartbeat;
	
} FRONTEND, * PFRONTEND;

// createFrontend
// this function creates a structure of a frontend
//
PFRONTEND createFrontend( char* username, char* password );

// deleteFrontend
// this function deletes a structure of a frontend
//
void deleteFrontend( PFRONTEND pfrontend );

// loginFrontend
// this function attempts to log in our frontend
//
bool loginFrontend( PFRONTEND pfrontend );

// logoutFrontend
// this function attempts to log out our frontend
//
bool logoutFrontend( PFRONTEND pfrontend );

#endif //FRONTEND_H
