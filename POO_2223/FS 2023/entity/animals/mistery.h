#ifndef MISTERY_H
#define MISTERY_H

#include "animal.h"

class Mistery : public Animal
{
public:
	Mistery( );
	
	void reproduce( ) override;
	
	void move( DIRECTION direction ) override;
	
	void processTick( ) override;
	
	void onDeath( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //MISTERY_H