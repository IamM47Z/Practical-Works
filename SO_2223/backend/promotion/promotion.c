#include <stdlib.h>
#include <string.h>

#include "promotion.h"

PPROMOTION createPromotion( char* category, unsigned int timer, float value, unsigned int promoter_id )
{
	if ( !category || timer <= 0 || value <= 0 || strlen( category ) > 24 )
		return NULL;
	
	// allocate promotion structure
	PPROMOTION ppromotion = calloc( 1, sizeof( PROMOTION ) );
	if ( !ppromotion )
		return NULL;
	
	strcpy( ppromotion->category, category );
	
	// initialize values
	ppromotion->pnext = NULL;
	ppromotion->value = value;
	ppromotion->timer = timer;
	ppromotion->promoter_id = promoter_id;
	
	return ppromotion;
}

void deletePromotions( PPROMOTION ppromotion )
{
	if ( !ppromotion )
		return;
	
	// delete all promotions on linked list
	while ( ppromotion )
		ppromotion = deletePromotion( ppromotion );
}

PPROMOTION deletePromotion( PPROMOTION ppromotion )
{
	if ( !ppromotion )
		return NULL;
	
	// delete promotion and return the next promotion
	PPROMOTION pnext = ppromotion->pnext;
	memset( ppromotion, 0, sizeof( PROMOTION ) );
	free( ppromotion );
	return pnext;
}

PPROMOTION getLastPromotion( PPROMOTION ppromotion )
{
	if ( !ppromotion )
		return NULL;
	
	// get last element from linked list
	while ( ppromotion->pnext )
		ppromotion = ppromotion->pnext;
	
	return ppromotion;
}

PPROMOTION getPromotionByCategory( PPROMOTION ppromotion, const char* category )
{
	if ( !ppromotion || !category )
		return NULL;
	
	PPROMOTION ret = NULL;
	float best_promo = 1.0f;
	while ( ppromotion )
	{
		if ( strcmp( ppromotion->category, category ) == 0 && ppromotion->value < best_promo )
		{
			ret = ppromotion;
			best_promo = ppromotion->value;
		}
		
		ppromotion = ppromotion->pnext;
	}
	
	return ret;
}