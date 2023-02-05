#ifndef IO_H
#define IO_H

#include <fcntl.h>
#include <limits.h>
#include <stdbool.h>

#include "../insparse/insparse.h"

typedef struct
{
	unsigned int value;
	float promotion_value;
	
	char username[25];
} BID;

typedef struct AUCTION
{
	BID current_bid;
	
	char name[25], category[25], seller_name[25];
	unsigned int buy_now_value, item_id, timer;
	
	struct AUCTION* pnext;
} AUCTION, * PAUCTION;

typedef enum
{
	REQ_LOGIN,      // login users
	REQ_SELL,       // sell <name> <category> <price> <buy-now-price> <duration>
	REQ_LIST,       // get items at sell
	REQ_TIME,       // time up
	REQ_BID,        // buy command
	REQ_BALANCE,    // cash
	REQ_ADD,        // add
	REQ_DISCONNECT, // disconnect
	REQ_HEARTBEAT,  // heartbeat
	REQ_PRINT
} REQ_TYPE;

typedef struct
{
	// Header
	//
	int from, to;             // -1 -> All Clients will Execute | 0 -> Server and the others are client PID's
	REQ_TYPE type;
	
	// Response
	//
	struct
	{
		bool status;                   // response status in case its applicable
		union
		{
			struct
			{
				char pathname[PATH_MAX], username[25], password[25];
			} login;
			
			struct
			{
				char name[25], category[25];
				unsigned int price, buy_now_price, duration;
			} sell;
			
			struct
			{
				unsigned int auction_id, value;
			} bid;
			
			struct
			{
				unsigned int value;
			} add;
			
			struct
			{
				bool by_cat: 1;
				bool by_user: 1;
				bool by_price: 1;
				bool by_time: 1;
				char cat[25], user[25];
				unsigned int price, time;
			} list;
		} front;
		union
		{
			struct
			{
				AUCTION auctions[30];   // simple way to send our auction list (this could be way better but the IPC mechanism is not the best)
				unsigned int num;
			} list;
			
			unsigned int uptime;
			
			unsigned int balance;
			
			char buffer[256];
			
			bool purchased;
		} back;
	} response;
} REQUEST, * PREQUEST;

typedef struct
{
	bool initialized;
	char pathname[PATH_MAX];
	
	int fd;
} IO, * PIO;

// initializeIo
// this function initializes the fifo
//
bool initializeIo( PIO pio, char* pathname, int flags );

// destroyIo
// this function destroys the server_io structure
//
void destroyIo( PIO pio );

// readIoFifo
// this function reads the IO fifo to buffer
//
int readIoFifo( PIO pio, char* buffer, size_t size );

// writeIoFifo
// this function writes the buffer to the IO fifo
//
int writeIoFifo( PIO pio, char* buffer, size_t size );

// toggleIoBlockable
// this function changes the IO flags to include O_NONBLOCK
//
bool toggleIoBlockable( PIO pio );

#endif //IO_H
