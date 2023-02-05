#ifndef WOLF_H
#define WOLF_H

#include "animal.h"

class Wolf : public Animal
{
	bool hunting = false;
public:
	Wolf( );
	
	void reproduce( ) override;
	
	void move( DIRECTION direction ) override;
	
	void processTick( ) override;
	
	void onDeath( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //WOLF_H
