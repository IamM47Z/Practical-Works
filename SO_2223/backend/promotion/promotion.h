#ifndef PROMOTION_H
#define PROMOTION_H

typedef struct PROMOTION
{
	char category[25];
	unsigned int timer, promoter_id;
	float value;
	
	struct PROMOTION* pnext;
} PROMOTION, * PPROMOTION;

// createPromotion
// this function initialize a promotion structure
//
PPROMOTION createPromotion( char* category, unsigned int timer, float value, unsigned int promoter_id );

// deletePromotion
// this function frees promotion memory
//
PPROMOTION deletePromotion( PPROMOTION ppromotion );

// deletePromotions
// this function deletes the whole linked list of promotions
//
void deletePromotions( PPROMOTION ppromotion );

// getLastPromotion
// this function gets the latest promotion on the linked list
//
PPROMOTION getLastPromotion( PPROMOTION ppromotion );

// getPromotionByCategory
// this function gets the best promotion on the linked list by his category name
//
PPROMOTION getPromotionByCategory( PPROMOTION ppromotion, const char* category );

#endif //PROMOTION_H
