#ifndef RABBIT_H
#define RABBIT_H

#include "animal.h"

class Rabbit : public Animal
{
public:
	Rabbit( );
	
	void reproduce( ) override;
	
	void move( DIRECTION direction ) override;
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //RABBIT_H
