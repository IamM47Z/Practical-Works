#ifndef BACKEND_H
#define BACKEND_H

#include <stdbool.h>
#include <pthread.h>

#include "../io/io.h"
#include "../client/client.h"
#include "../auction/auction.h"
#include "../promoter/promoter.h"
#include "../promotion/promotion.h"

typedef struct
{
	IO io;
	
	CLIENT pclients[20];                        // could be better but have too much work to do in other subjects...
	PAUCTION pauctions;
	PPROMOTER ppromoters;
	PPROMOTION ppromotions;
	
	pthread_mutex_t clients_mutex;
	pthread_mutex_t auctions_mutex;
	pthread_mutex_t promoters_mutex;
	pthread_mutex_t promotions_mutex;
	
	pthread_t time_thread;
	pthread_t worker_thread;
	
	int announcements_pipe[2];
	
	int uptime;
	unsigned int heartbeat;
	char cur_executable_path[PATH_MAX + 1];
	unsigned int num_users, num_online_clients;
} BACKEND, * PBACKEND;

// createBackend
// this function creates a backend structure
//
PBACKEND createBackend( );

// deleteBackend
// this function deletes a backend structure
//
void deleteBackend( PBACKEND pbackend );

// startBackendPromoters
// this function starts the backend promoters
//
bool startBackendPromoters( PBACKEND pbackend );

// stopBackendPromoters
// this function stop the backend promoters. returns the number of promoters stopped
//
int stopBackendPromoters( PBACKEND pbackend );

// processBackendPromoters
// this function reads the backend promoters
//
int processBackendPromoters( PBACKEND pbackend );

// processBackendPromotions
// this function cleans up finished promotions
//
int processBackendPromotions( PBACKEND pbackend );

// processBackendAuctions
// this function processes a second on each Auction
//
int processBackendAuctions( PBACKEND pbackend );

// processBackendClients
// this function processes the heartbeat
//
int processBackendClients( PBACKEND pbackend );

// getClientByPid
// this function gets a client structure by his process id
//
PCLIENT getClientByPid( PCLIENT pclients, unsigned int num_clients, unsigned int pid );

// getClientByUsername
// this function gets a client structure by his username
//
PCLIENT getClientByUsername( PCLIENT pclients, unsigned int num_clients, char* username );

// updateClientHeartbeat
// this function refreshes a client heartbeat
//
bool updateClientHeartbeat( PBACKEND pbackend, unsigned int pid );

// bidBackendAuction
// this function sets a bid on a backend auction
//
bool bidBackendAuction( PBACKEND pbackend, PAUCTION pauction, PCLIENT pclient, unsigned int value );

// buyNowBackendAuction
// this function buys a backend auction now
//
bool buyNowBackendAuction( PBACKEND pbackend, PAUCTION pauction, PCLIENT pclient, unsigned int value );

#endif //BACKEND_H
