#ifndef RESERVE_H
#define RESERVE_H

#include "camera/camera.h"

#include "../entity/foods/foods.h"
#include "../entity/animals/animals.h"

class Reserve : public Camera
{
	COORD_PAIR max_coords;
	std::vector< EntityPtr > entity_list;

public:
	Reserve( int x, int y ) : Camera( x, y )
	{
		this->max_coords = std::make_pair( x, y );
	}
	
	~Reserve( );
	
	COORD_PAIR getMaxCoords( );
	
	void processUpdates( );
	
	void processTicks( int nticks = 1, int delay = 0 );
	
	EntityPtr getEntityById( int id );
	
	AnimalPtr createAnimal( const std::string& specie_initial, COORD_PAIR location = { 0, 0 } );
	
	void killAnimal( const AnimalPtr& panimal );
	
	FoodPtr growFood( const std::string& specie_initial, COORD_PAIR location = { 0, 0 } );
	
	void removeFood( const FoodPtr& pfood );
	
	bool addEntity( const EntityPtr& pentity );
	
	void removeEntity( int entity_id );
	
	void forEachEntity( const std::function< bool( EntityPtr ) >& callback );
	
	Reserve getClone( );
};

#endif //RESERVE_H
