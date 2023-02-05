#ifndef KANGAROO_H
#define KANGAROO_H

#include "animal.h"

class Kangaroo : public Animal
{
	// child
	int in_bag = 0;
	bool afraid = false;
	
	// adult
	std::vector< std::shared_ptr< Kangaroo >> childs_in_bag;
public:
	Kangaroo( );
	
	void reproduce( ) override;
	
	void move( DIRECTION direction ) override;
	
	void processTick( ) override;
	
	void onDeath( ) override;
	
	[[nodiscard]] bool isInBag( ) const;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //KANGAROO_H
