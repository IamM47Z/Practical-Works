#include "grass.h"

#include "../../utils/utils.h"
#include "../../consts/consts.h"
#include "../../engine/engine.h"

Grass::Grass( ) : Food( "Grass" )
{
	visual_representation = 'r';
	
	if ( !consts::getConst( "VRelva", duration ) )
	{
		endwin( );
		perror( "Constants could not be found" );
		exit( 1 );
	}
	
	toxicity_points = 0;
	nutritive_points = 3;
	
	if ( !addSmell( SMELL::ERVA ) || !addSmell( SMELL::VERDURA ) )
	{
		endwin( );
		perror( "Fatal error initializing Grass Smells" );
		exit( 1 );
	}
}

void Grass::processTick( )
{
	const auto preserve = engine::getReserve( );
	
	age++;
	
	if ( !reproduced && age >= static_cast<int>( 0.75 * duration ) )
	{
		auto child_coords = this->coords;
		child_coords.first += utils::genRandomNum( 4, 8 ) * ( utils::genRandomNum( 0, 1 ) ? -1 : 1 );
		child_coords.second += utils::genRandomNum( 4, 8 ) * ( utils::genRandomNum( 0, 1 ) ? -1 : 1 );
		
		auto pchild = std::make_shared< Grass >( );
		if ( pchild->moveTo( child_coords ) )
		{
			if ( !engine::getReserve( )->addEntity( pchild ) )
			{
				endwin( );
				perror( "Fatal error Adding Entity to Entity List" );
				exit( 1 );
			}
			
			reproduced = true;
		}
	}
}

std::shared_ptr< Entity > Grass::getClone( )
{
	auto pclone = std::make_shared< Grass >( );
	
	// entity class
	//
	pclone->id = id;
	pclone->alive = alive;
	
	pclone->range = range;
	pclone->visual_representation = visual_representation;
	pclone->coords = coords;
	
	// foodentry class
	//
	pclone->species = species;
	pclone->nutritive_points = nutritive_points;
	pclone->toxicity_points = toxicity_points;
	
	// food class
	//
	pclone->age = age;
	pclone->duration = duration;
	pclone->smells = smells;
	
	// grass class
	pclone->reproduced = reproduced;
	
	return pclone;
}