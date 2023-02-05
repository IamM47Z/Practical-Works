#include "misteryfood.h"

#include "../../utils/utils.h"
#include "../../consts/consts.h"
#include "../../engine/engine.h"

MisteryFood::MisteryFood( ) : Food( "Mistery" )
{
	visual_representation = 'a';
	
	toxicity_points = ( utils::genRandomNum( 0, 100 ) <= 25 ? 0 : 1 );
	nutritive_points = ( toxicity_points == 0 ? 100 : utils::genRandomNum( 0, 20 ) );
	
	// all smells
	//
	if ( !addSmell( SMELL::ERVA ) || !addSmell( SMELL::VERDURA ) ||
	     !addSmell( SMELL::CARNE ) || !addSmell( SMELL::KETCHUP ) )
	{
		endwin( );
		perror( "Fatal error initializing Grass Smells" );
		exit( 1 );
	}
}

void MisteryFood::processTick( )
{
	const auto preserve = engine::getReserve( );
	
	age++;
	
	if ( nutritive_points > 0 )
		nutritive_points--;
	
	if ( toxicity_points > 0 )
		toxicity_points++;
	
	if ( !( age % 30 ) )
	{
		auto child_coords = this->coords;
		child_coords.first += utils::genRandomNum( 4, 8 ) * ( utils::genRandomNum( 0, 1 ) ? -1 : 1 );
		child_coords.second += utils::genRandomNum( 4, 8 ) * ( utils::genRandomNum( 0, 1 ) ? -1 : 1 );
		
		auto pchild = std::make_shared< MisteryFood >( );
		if ( pchild->moveTo( child_coords ) )
		{
			if ( !engine::getReserve( )->addEntity( pchild ) )
			{
				endwin( );
				perror( "Fatal error Adding Entity to Entity List" );
				exit( 1 );
			}
		}
	}
}

std::shared_ptr< Entity > MisteryFood::getClone( )
{
	auto pclone = std::make_shared< MisteryFood >( );
	
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
	
	return pclone;
}