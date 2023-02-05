#ifndef CLIENT_H
#define CLIENT_H

#include <stdint.h>
#include <stdbool.h>

#include "../io/io.h"

typedef struct
{
	IO io;
	
	int pid;
	char username[25];
	time_t last_hb_timestamp;
} CLIENT, * PCLIENT;

// addClient
// this function adds a client
//
bool addClient( PCLIENT client_list, unsigned int num_clients, char* pathname, char* username, char* password,
                int pid );

// removeClient
// this function removes a client
//
bool removeClient( PCLIENT client_list, unsigned int num_clients, int pid );

#endif //CLIENT_H