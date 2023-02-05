#ifndef SHEEP_H
#define SHEEP_H

#include "animal.h"

class Sheep : public Animal
{
public:
	Sheep( );
	
	void onDeath( ) override;
	
	void reproduce( ) override;
	
	void move( DIRECTION direction ) override;
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};


#endif //SHEEP_H
