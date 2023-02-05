#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

#include "../io/io.h"

#include "auction.h"

PAUCTION createAuction( unsigned int id, char* name, char* category, char* seller_name, unsigned int base_value,
                        unsigned int buy_now_value, unsigned int timer )
{
	// validate values
	if ( strlen( name ) > 24 || strlen( category ) > 24 || strlen( seller_name ) > 24 )
		return NULL;
	
	// allocate auction structure
	PAUCTION pauction = calloc( 1, sizeof( AUCTION ) );
	if ( !pauction )
		return NULL;
	
	strcpy( pauction->name, name );
	strcpy( pauction->category, category );
	strcpy( pauction->seller_name, seller_name );
	
	pauction->current_bid.value = base_value;
	pauction->current_bid.promotion_value = 1.0f;
	strcpy( pauction->current_bid.username, "-" );
	
	pauction->item_id = id;
	pauction->timer = timer;
	pauction->buy_now_value = buy_now_value;
	
	pauction->pnext = NULL;
	return pauction;
}

PAUCTION deleteAuction( PAUCTION pauction )
{
	if ( !pauction )
		return NULL;
	
	PAUCTION pnext = pauction->pnext;
	memset( pauction, 0, sizeof( AUCTION ) );
	free( pauction );
	return pnext;
}

inline bool isAuctionFinished( PAUCTION pauction )
{
	return !pauction->timer;
}

PAUCTION loadAuctions( char* path )
{
	if ( !path )
		return NULL;
	
	// load auctions
	char* fbuffer;
	int read_bytes = readFile( path, &fbuffer );
	if ( read_bytes == -1 )
		return NULL;
	
	PAUCTION pauctions = NULL;
	for ( char* line = strtok( fbuffer, "\n" ); line; line = strtok( NULL, "\n" ) )
	{
		static PAUCTION plast = NULL;
		static char name[25], category[25], seller_name[25], last_bidder_name[25];
		
		// read strings
		if ( sscanf( line, "%*d %s %s %*d %*d %*d %s %s",
		             name, category, seller_name, last_bidder_name ) != 4 )
			return NULL;
		
		// read id
		unsigned int id = strtol( line, &line, 10 );
		
		// skip name and category
		line += 1 + strlen( name ) + 1 + strlen( category ) + 1;
		
		// read other information
		unsigned int value = strtol( line, &line, 10 );
		unsigned int buy_now_value = strtol( line, &line, 10 );
		unsigned int time = strtol( line, &line, 10 );
		
		// create auction
		PAUCTION pauction = createAuction( id, name, category, seller_name, value, buy_now_value, time );
		if ( !pauction )
			return NULL;
		
		// set the bidder name
		strcpy( pauction->current_bid.username, last_bidder_name );
		
		// add auction to linked list
		if ( !pauctions )
			pauctions = pauction;
		else
			plast->pnext = pauction;
		
		plast = pauction;
	}
	
	free( fbuffer );
	
	return pauctions;
}

bool saveAuctions( PAUCTION pauctions, char* path )
{
	if ( !path )
		return false;
	
	FILE* pfile = fopen( path, "w" );
	if ( !pfile )
	{
		perror( "Error opening/creating the auctions file" );
		return false;
	}
	
	for ( PAUCTION pauction = pauctions; pauction; pauction = pauction->pnext )
		fprintf( pfile, "%d %s %s %d %d %d %s %s\n",
		         pauction->item_id, pauction->name, pauction->category, getAuctionFinalPrice( pauction ),
		         pauction->buy_now_value, pauction->timer, pauction->seller_name, pauction->current_bid.username );
	
	fclose( pfile );
	
	return true;
}

void deleteAuctions( PAUCTION pauction )
{
	if ( !pauction )
		return;
	
	// delete all auctions on linked list
	while ( pauction )
		pauction = deleteAuction( pauction );
}

PAUCTION getLastAuction( PAUCTION pauction )
{
	if ( !pauction )
		return NULL;
	
	// get last element from linked list
	while ( pauction->pnext )
		pauction = pauction->pnext;
	
	return pauction;
}

unsigned int getMaxAuctionId( PAUCTION pauction )
{
	if ( !pauction )
		return 1;
	
	unsigned int max_id = pauction->item_id;
	for ( pauction = pauction->pnext; pauction; pauction = pauction->pnext )
		if ( pauction->item_id > max_id )
			max_id = pauction->item_id;
	
	return max_id;
}

PAUCTION getAuctionById( PAUCTION pauction, unsigned int id )
{
	if ( !pauction )
		return NULL;
	
	while ( pauction )
	{
		if ( pauction->item_id == id )
			return pauction;
		
		pauction = pauction->pnext;
	}
	
	return NULL;
}

int getAuctionFinalPrice( PAUCTION pauction )
{
	if ( !pauction )
		return -1;
	
	return ( int ) roundf( ( float ) ( pauction->current_bid.value ) * pauction->current_bid.promotion_value );
}