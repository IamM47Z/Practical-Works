#ifndef PROMOTER_H
#define PROMOTER_H

#include <limits.h>

typedef struct PROMOTER
{
	unsigned int id;
	
	int pipe[2];
	pid_t process_id;
	
	char path[PATH_MAX + 1];
	struct PROMOTER* pnext;
} PROMOTER, * PPROMOTER;

// createPromoter
// this function initialize a promoter structure
//
PPROMOTER createPromoter( unsigned int id, char* file_name );

// deletePromoter
// this function frees promoter memory
//
PPROMOTER deletePromoter( PPROMOTER ppromoter );

// deletePromoters
// this function deletes the whole linked list of promoters
//
void deletePromoters( PPROMOTER ppromoter );

// getLastPromoter
// this function gets the latest promoter on the linked list
//
PPROMOTER getLastPromoter( PPROMOTER ppromoter );

// startPromoter
// this function starts the promoter
//
bool startPromoter( PPROMOTER ppromoter );

// stopPromoter
// this function stop the promoter
//
bool stopPromoter( PPROMOTER ppromoter );

// isPromoterRunning
// this function checks if a promoter is running
//
bool isPromoterRunning( PPROMOTER ppromoter );

// readPromoter
// this function reads the promoter pipe (pbuffer -> out)
//
int readPromoter( PPROMOTER ppromoter, char** pbuffer );

// loadPromoters
// this function loads the promoters from the disk
//
PPROMOTER loadPromoters( char* path, char* promoters_dir );

// reloadPromoters
// this function loads the promoters from the disk
//
bool reloadPromoters( PPROMOTER* pppromoters, char* path, char* promoters_dir );

// getPromoterFileName
// this function gets a promoter file name
//
char* getPromoterFileName( PPROMOTER ppromoter );

// getMinAvPromoterId
// this function gets the minimum available promoter id
//
unsigned int getMinAvPromoterId( PPROMOTER ppromoter );

#endif //PROMOTER_H
