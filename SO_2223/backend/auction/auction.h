#ifndef AUCTION_H
#define AUCTION_H

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

// createAuction
// this function initializes the auction structure
//
PAUCTION createAuction( unsigned int id, char* name, char* category, char* seller_name, unsigned int value,
                        unsigned int buy_now_value, unsigned int timer );

// deleteAuction
// this function free's the memory allocated on an auction structure
//
PAUCTION deleteAuction( PAUCTION pauction );

// deleteAuctions
// this function free's the memory allocated for the whole auction linked list
//
void deleteAuctions( PAUCTION pauction );

// isAuctionFinished
// this function retrieves whether the auction is finished or no
//
bool isAuctionFinished( PAUCTION pauction );

// loadAuctions
// this function loads the auctions from the disk
//
PAUCTION loadAuctions( char* path );

// saveAuctions
// this function saves the auctions on the disk
//
bool saveAuctions( PAUCTION pauctions, char* path );

// getLastAuction
// this function gets the latest auction on the linked list
//
PAUCTION getLastAuction( PAUCTION pauction );

// getMaxAuctionId
// this function gets the minimum id available from the maximum auction id
//
unsigned int getMaxAuctionId( PAUCTION pauction );

// getAuctionById
// this function gets an auction by ID
//
PAUCTION getAuctionById( PAUCTION pauction, unsigned int id );

// getAuctionFinalPrice
// this function gets his final price
//
int getAuctionFinalPrice( PAUCTION pauction );

#endif //AUCTION_H
