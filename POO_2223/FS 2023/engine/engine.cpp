#include <sstream>
#include <fstream>
#include <iostream>
#include <algorithm>

#include "engine.h"

#include "../utils/utils.h"
#include "../consts//consts.h"
#include "../insparse/insparse.h"

namespace engine
{
	namespace
	{
		Reserve* current_reserve;
		std::map< std::string, Reserve > saved_reserves;
	}
	
	bool initialize( )
	{
		if ( !consts::isLoaded( ) && !consts::loadConsts( "constantes.txt" ) )
			return false;
		
		auto nlines = utils::genRandomNum( 16, 500 );
		auto ncolumns = utils::genRandomNum( 16, 500 );
		current_reserve = new Reserve( ncolumns, nlines );
		return true;
	}
	
	void reset( )
	{
		delete current_reserve;
		current_reserve = nullptr;
	}
	
	Reserve* getReserve( )
	{
		return current_reserve;
	}
	
	void setReserve( Reserve& reserve )
	{
		reset( );
		current_reserve = new Reserve( reserve.getClone( ) );
	}
	
	void render( )
	{
		static bool initialized = false;
		if ( !initialized )
		{
			initialized = true;
			current_reserve->reset( );
		}
		
		const auto max_coords = current_reserve->getMaxCoords( );
		
		std::ostringstream buffer;
		buffer << "Reserve " << max_coords.first << "x" << max_coords.second;
		
		auto& terminal = term::Terminal::instance( );
		terminal << term::move_to( 0, 0 ) << "Farming Simulator 2023"
		         << term::move_to( term::Terminal::getNumCols( ) - static_cast<int>(buffer.str( ).length( )), 0 )
		         << buffer.str( );
		
		current_reserve->processUpdates( );
		current_reserve->render( );
	}

#pragma clang diagnostic push
#pragma ide diagnostic ignored "misc-no-recursion"
	
	void processCommand( term::Window& hwnd, INS_INFO info )
	{
		// need for lambda functions
		//
		auto preserve = current_reserve;
		
		switch ( info.type )
		{
			case ANIMAL:
			{
				const auto max_coords = preserve->getMaxCoords( );
				auto pos_x = ( info.args.size( ) == 3 ) ? std::stoi( info.args[ 2 ] )
				                                        : utils::genRandomNum(
								1, max_coords.first );
				auto pos_y = ( info.args.size( ) == 3 ) ? std::stoi( info.args[ 1 ] )
				                                        : utils::genRandomNum(
								1, max_coords.second );
				
				if ( pos_x > max_coords.first || pos_x < 1 )
				{
					hwnd << "Invalid X Coordinate: " << pos_x << "\n";
					break;
				}
				else if ( pos_y > max_coords.second || pos_y < 1 )
				{
					hwnd << "Invalid Y Coordinate: " << pos_y << "\n";
					break;
				}
				
				hwnd << "Creating Animal " << info.args[ 0 ] << " at (" << pos_x << ", " << pos_y
				     << ")\n";
				
				AnimalPtr ptr_animal = preserve->createAnimal( info.args[ 0 ], std::make_pair( pos_x, pos_y ) );
				if ( ptr_animal == nullptr )
					hwnd << "Error creating the Animal! Error: " << strerror( errno ) << "\n";
				else
					hwnd << "Animal created sucessfuly! ID: " << ptr_animal->getId( ) << "\n";
				
				break;
			}
			case KILL:
			{
				const auto max_coords = preserve->getMaxCoords( );
				auto pos_x = std::stoi( info.args[ 1 ] );
				auto pos_y = std::stoi( info.args[ 0 ] );
				
				if ( pos_x > max_coords.first || pos_x < 1 )
				{
					hwnd << "Invalid X Coordinate: " << pos_x << "\n";
					break;
				}
				else if ( pos_y > max_coords.second || pos_y < 1 )
				{
					hwnd << "Invalid Y Coordinate: " << pos_y << "\n";
					break;
				}
				
				std::vector< AnimalPtr > animals;
				auto location = std::make_pair( pos_x, pos_y );
				preserve->forEachEntity( [ &hwnd, location, &animals ]( const EntityPtr& entity )
				                         {
					                         if ( entity->getCoords( ) == location &&
					                              std::dynamic_pointer_cast< Animal >( entity ) )
					                         {
						                         const auto panimal = std::static_pointer_cast< Animal >( entity );
						
						                         hwnd << "Killed Animal at (" << location.first << ", "
						                              << location.second << "). ID: " << panimal->getId( ) << "\n";
						
						                         animals.emplace_back( panimal );
					                         }
					
					                         return true;
				                         } );
				
				for ( auto& panimal: animals )
					preserve->killAnimal( panimal );
				
				break;
			}
			case KILLID:
			{
				auto entity = preserve->getEntityById( std::stoi( info.args[ 0 ] ) );
				if ( !entity || !std::dynamic_pointer_cast< Animal >( entity ) )
				{
					hwnd << "Invalid Animal ID\n";
					break;
				}
				
				auto panimal = std::static_pointer_cast< Animal >( entity );
				auto location = panimal->getCoords( );
				hwnd << "Killed Animal with ID: " << panimal->getId( ) << ". Located at (" << location.first << ", "
				     << location.second << ")\n";
				
				preserve->killAnimal( panimal );
				break;
			}
			case FOOD:
			{
				const auto max_coords = preserve->getMaxCoords( );
				auto pos_x = ( info.args.size( ) == 3 ) ? std::stoi( info.args[ 2 ] )
				                                        : utils::genRandomNum(
								1, max_coords.first );
				auto pos_y = ( info.args.size( ) == 3 ) ? std::stoi( info.args[ 1 ] )
				                                        : utils::genRandomNum(
								1, max_coords.second );
				
				if ( pos_x > max_coords.first || pos_x < 1 )
				{
					hwnd << "Invalid X Coordinate: " << pos_x << "\n";
					break;
				}
				else if ( pos_y > max_coords.second || pos_y < 1 )
				{
					hwnd << "Invalid Y Coordinate: " << pos_y << "\n";
					break;
				}
				
				FoodPtr pfood = preserve->growFood( info.args[ 0 ], std::make_pair( pos_x, pos_y ) );
				if ( pfood == nullptr )
					hwnd << "There is already food at (" << pos_x << ", " << pos_y << ")\n";
				else
					hwnd << "Food planted sucessfuly at (" << pfood->getCoords( ).first << ", "
					     << pfood->getCoords( ).second << ")! ID: " << pfood->getId( ) << "\n";
				
				break;
			}
			case FEED:
			{
				const auto max_coords = preserve->getMaxCoords( );
				auto pos_x = std::stoi( info.args[ 1 ] );
				auto pos_y = std::stoi( info.args[ 0 ] );
				
				if ( pos_x > max_coords.first || pos_x < 1 )
				{
					hwnd << "Invalid X Coordinate: " << pos_x << "\n";
					break;
				}
				else if ( pos_y > max_coords.second || pos_y < 1 )
				{
					hwnd << "Invalid Y Coordinate: " << pos_y << "\n";
					break;
				}
				
				auto nutritive = std::stoi( info.args[ 2 ] );
				auto toxicity = std::stoi( info.args[ 3 ] );
				
				if ( nutritive < 0 )
				{
					hwnd << "Invalid nutritive points. Value must be positive.\n";
					break;
				}
				if ( toxicity < 0 )
				{
					hwnd << "Invalid toxicity points. Value must be positive.\n";
					break;
				}
				
				auto status = false;
				auto location = std::make_pair( pos_x, pos_y );
				preserve->forEachEntity( [ &status, &hwnd, location, nutritive, toxicity ]( const EntityPtr& entity )
				                         {
					                         if ( entity->getCoords( ) == location &&
					                              std::dynamic_pointer_cast< Animal >( entity ) )
					                         {
						                         auto panimal = std::static_pointer_cast< Animal >(
								                         entity );
						                         auto coords = panimal->getCoords( );
						
						                         panimal->eat( { "user", nutritive, toxicity } );
						
						                         hwnd << "The animal of species " << panimal->getSpecies( ) << " at ("
						                              << coords.first << ", "
						                              << coords.second << ") has been feeded!\n";
						
						                         if ( panimal->getHealth( ) <= 0 )
							                         panimal->remove( );
						
						                         status = true;
					                         }
					
					                         return true;
				                         } );
				
				if ( !status )
					hwnd << "There is no animal at (" << pos_x << ", " << pos_y << ")\n";
				
				break;
			}
			case FEEDID:
			{
				auto entity = preserve->getEntityById( std::stoi( info.args[ 0 ] ) );
				if ( !entity || !std::dynamic_pointer_cast< Animal >( entity ) )
				{
					hwnd << "Invalid Animal ID\n";
					break;
				}
				
				auto nutritive = std::stoi( info.args[ 1 ] );
				auto toxicity = std::stoi( info.args[ 2 ] );
				
				if ( nutritive < 0 )
				{
					hwnd << "Invalid nutritive points. Value must be positive.\n";
					break;
				}
				if ( toxicity < 0 )
				{
					hwnd << "Invalid toxicity points. Value must be positive.\n";
					break;
				}
				
				auto panimal = std::static_pointer_cast< Animal >( entity );
				panimal->eat( { "user", nutritive, toxicity } );
				
				hwnd << "The animal of species " << panimal->getSpecies( ) << " with ID: " << panimal->getId( )
				     << " has been feeded!\n";
				
				if ( panimal->getHealth( ) <= 0 )
					preserve->killAnimal( panimal );
				
				break;
			}
			case NOFOOD:
			{
				EntityPtr pentity = nullptr;
				
				if ( info.args.size( ) == 1 )
					pentity = preserve->getEntityById( std::stoi( info.args[ 0 ] ) );
				else
				{
					const auto location = std::make_pair( std::stoi( info.args[ 1 ] ),
					                                      std::stoi( info.args[ 0 ] ) );
					preserve->forEachEntity( [ &pentity, location ]( const EntityPtr& entity )
					                         {
						                         if ( std::dynamic_pointer_cast< Food >( entity ) &&
						                              entity->getCoords( ) == location )
						                         {
							                         pentity = entity;
							                         return false;
						                         }
						                         return true;
					                         } );
				}
				
				if ( !pentity || !std::dynamic_pointer_cast< Food >( pentity ) )
				{
					hwnd << "Invalid Food " << ( info.args.size( ) == 1 ? "ID" : "Coordinates" ) << "\n";
					break;
				}
				
				auto pfood = std::static_pointer_cast< Food >( pentity );
				preserve->removeFood( pfood );
				
				if ( info.args.size( ) == 1 )
					hwnd << "Removed " << pfood->getVisualChar( ) << " with ID: "
					     << pfood->getId( ) << "\n";
				else
					hwnd << "Removed " << pfood->getVisualChar( ) << " at ("
					     << pfood->getCoords( ).first << ", " << pfood->getCoords( ).second << ")\n";
				break;
			}
			case EMPTY:
			{
				const auto max_coords = preserve->getMaxCoords( );
				auto pos_x = std::stoi( info.args[ 1 ] );
				auto pos_y = std::stoi( info.args[ 0 ] );
				
				if ( pos_x > max_coords.first || pos_x < 1 )
				{
					hwnd << "Invalid X Coordinate: " << pos_x << "\n";
					break;
				}
				else if ( pos_y > max_coords.second || pos_y < 1 )
				{
					hwnd << "Invalid Y Coordinate: " << pos_y << "\n";
					break;
				}
				
				auto status = false;
				std::vector< FoodPtr > foods;
				std::vector< AnimalPtr > animals;
				auto location = std::make_pair( pos_x, pos_y );
				preserve->forEachEntity( [ &status, &hwnd, location, &foods, &animals ]( const EntityPtr& entity )
				                         {
					                         auto coords = entity->getCoords( );
					                         if ( coords.first == location.first &&
					                              coords.second == location.second )
					                         {
						                         if ( std::dynamic_pointer_cast< Animal >( entity ) )
						                         {
							                         auto panimal = std::static_pointer_cast< Animal >(
									                         entity );
							                         hwnd << "Removed animal of species " << panimal->getSpecies( )
							                              << " with ID: " << panimal->getId( ) << "\n";
							                         animals.emplace_back( panimal );
						                         }
						                         else if ( std::dynamic_pointer_cast< Food >( entity ) )
						                         {
							                         auto pfood = std::static_pointer_cast< Food >(
									                         entity );
							                         hwnd << "Removed food of species" << pfood->getSpecies( )
							                              << " with ID: " << pfood->getId( ) << "\n";
							                         foods.emplace_back( pfood );
						                         }
						                         else
						                         {
							                         endwin( );
							                         std::cout << "Error: Invalid Entity Pointer\n";
							                         exit( 1 );
						                         }
						
						                         status = true;
					                         }
					
					                         return true;
				                         } );
				
				if ( !status )
				{
					hwnd << "No Entity at (" << location.first << ", " << location.second << ")\n";
					break;
				}
				
				for ( const auto& pfood: foods )
					preserve->removeFood( pfood );
				
				for ( const auto& panimal: animals )
					preserve->killAnimal( panimal );
				
				break;
			}
			case SEE:
			{
				const auto max_coords = preserve->getMaxCoords( );
				auto pos_x = std::stoi( info.args[ 1 ] );
				auto pos_y = std::stoi( info.args[ 0 ] );
				
				if ( pos_x > max_coords.first || pos_x < 1 )
				{
					hwnd << "Invalid X Coordinate: " << pos_x << "\n";
					break;
				}
				else if ( pos_y > max_coords.second || pos_y < 1 )
				{
					hwnd << "Invalid Y Coordinate: " << pos_y << "\n";
					break;
				}
				
				auto status = false;
				auto location = std::make_pair( pos_x, pos_y );
				preserve->forEachEntity( [ &hwnd, &status, location ]( const EntityPtr& entity )
				                         {
					                         if ( entity->getCoords( ) == location )
					                         {
						                         if ( std::dynamic_pointer_cast< Animal >( entity ) )
						                         {
							                         auto panimal = std::static_pointer_cast< Animal >(
									                         entity );
							                         hwnd << "Animal of species " << panimal->getSpecies( )
							                              << " with ID: " << panimal->getId( ) << " and "
							                              << panimal->getHealth( ) << "HP\n";
						                         }
						                         else if ( std::dynamic_pointer_cast< Food >( entity ) )
						                         {
							                         auto pfood = std::static_pointer_cast< Food >(
									                         entity );
							                         hwnd << "Food of species " << pfood->getSpecies( ) << " with ID: "
							                              << pfood->getId( ) << ", "
							                              << pfood->getNutritive( ) << " Nutritive Points and "
							                              << pfood->getToxicity( ) << " Toxicity Points\n";
						                         }
						                         else
							                         hwnd << "Invalid Entity Found\n";
						
						                         status = true;
					                         }
					
					                         return true;
				                         } );
				
				if ( !status )
					hwnd << "There is no Entity at (" << pos_x << ", " << pos_y << ")\n";
				
				break;
			}
			case INFO:
			{
				auto entity = preserve->getEntityById( std::stoi( info.args[ 0 ] ) );
				if ( !entity )
				{
					hwnd << "Invalid Entity ID\n";
					break;
				}
				
				const auto entity_coords = entity->getCoords( );
				
				if ( std::dynamic_pointer_cast< Animal >( entity ) )
				{
					auto panimal = std::static_pointer_cast< Animal >(
							entity );
					hwnd << "Animal of species " << panimal->getSpecies( ) << " at ("
					     << entity_coords.first << ", " << entity_coords.second << ") with "
					     << panimal->getHealth( ) << "HP\n";
				}
				else if ( std::dynamic_pointer_cast< Food >( entity ) )
				{
					auto pfood = std::static_pointer_cast< Food >(
							entity );
					hwnd << "Food of species " << pfood->getSpecies( ) << " at ("
					     << entity_coords.first << ", " << entity_coords.second << ") with ID: "
					     << pfood->getId( ) << ", "
					     << pfood->getNutritive( ) << " Nutritive Points and "
					     << pfood->getToxicity( ) << " Toxicity Points\n";
				}
				else
				{
					endwin( );
					std::cout << "Error: Invalid Entity Pointer";
					exit( 1 );
				}
				break;
			}
			case NEXT:
			{
				auto nticks = info.args.empty( ) ? 1 : std::stoi( info.args[ 0 ] );
				current_reserve->processTicks( nticks,
				                               info.args.size( ) == 2 ? std::stoi( info.args[ 1 ] ) : 0 );
				
				hwnd << nticks << " Ticks Processed Successfully!\n";
				break;
			}
			case ANIM:
			{
				auto status = false;
				preserve->forEachEntity( [ &hwnd, &status ]( const EntityPtr& entity )
				                         {
					                         if ( std::dynamic_pointer_cast< Animal >( entity ) )
					                         {
						                         const auto location = entity->getCoords( );
						                         auto panimal = std::static_pointer_cast< Animal >(
								                         entity );
						                         hwnd << "Animal of species " << panimal->getSpecies( ) << " at ("
						                              << location.first << ", " << location.second
						                              << ") with ID: " << panimal->getId( ) << " and "
						                              << panimal->getHealth( ) << "HP\n";
						
						                         status = true;
					                         }
					
					                         return true;
				                         } );
				
				if ( !status )
					hwnd << "There are no animals at the Reserve\n";
				
				break;
			}
			case VISANIM:
			{
				auto status = false;
				preserve->forEachEntity( [ &hwnd, &status, preserve ]( const EntityPtr& entity )
				                         {
					                         if ( std::dynamic_pointer_cast< Animal >( entity ) &&
					                              preserve->isVisibleArea( entity->getCoords( ) ) )
					                         {
						                         auto panimal = std::static_pointer_cast< Animal >(
								                         entity );
						                         hwnd << "Animal of species " << panimal->getSpecies( ) << " (ID: "
						                              << panimal->getId( ) << ") with "
						                              << panimal->getHealth( ) << "HP\n";
						
						                         status = true;
					                         }
					
					                         return true;
				                         } );
				
				if ( !status )
					hwnd << "There are no animals at the Reserve\n";
				
				break;
			}
			case STORE:
			{
				if ( saved_reserves.find( info.args[ 0 ] ) == saved_reserves.end( ) )
				{
					saved_reserves.insert( { info.args[ 0 ], preserve->getClone( ) } );
					hwnd << "Reserve stored as " << info.args[ 0 ] << " successfully!\n";
				}
				else
					hwnd << "Reserve already stored!\n";
				
				break;
			}
			case RESTORE:
			{
				if ( saved_reserves.find( info.args[ 0 ] ) != saved_reserves.end( ) )
				{
					engine::setReserve( saved_reserves.find( info.args[ 0 ] )->second );
					hwnd << "Reserve " << info.args[ 0 ] << " restored successfully!\n";
				}
				else
					hwnd << "Reserve not found!\n";
				
				break;
			}
			case LOAD:
			{
				// read file
				std::ifstream file( info.args[ 0 ] );
				if ( !file.is_open( ) )
				{
					hwnd << "Error reading file\n";
					break;
				}
				
				// read line
				std::string line{ };
				while ( std::getline( file, line ) )
				{
					INS_INFO file_info{ };
					if ( !insparse::parseInstruction( hwnd, line, file_info ) )
						continue;
					
					processCommand( hwnd, file_info );
					render( );
				}
				
				file.close( );
				
				hwnd << "\nFile loaded successfully!\n";
				break;
			}
			case SLIDE:
			{
				DIRECTION direction{ };
				if ( utils::istrcmp( info.args[ 0 ], "up" ) )
					direction.up = true;
				else if ( utils::istrcmp( info.args[ 0 ], "down" ) )
					direction.down = true;
				else if ( utils::istrcmp( info.args[ 0 ], "left" ) )
					direction.left = true;
				else if ( utils::istrcmp( info.args[ 0 ], "right" ) )
					direction.right = true;
				else
				{
					hwnd << "Invalid direction\n";
					break;
				}
				
				if ( preserve->moveVisibleArea( direction, std::stoi( info.args[ 1 ] ) ) )
				{
					hwnd << "Successfully slided the Visible Area " << info.args[ 1 ] << " Steps\n";
					break;
				}
				
				hwnd << "Invalid number of Steps\nMake sure there is enough room in the Reserve!\n";
				break;
			}
			case HELP:
				insparse::showHelp( hwnd );
				break;
			case EXIT:
				hwnd.move( 0, 0 );
				hwnd << "Exiting\n";
				break;
			default:
				break;
		}
	}

#pragma clang diagnostic pop
	
	bool processKeys( )
	{
		return current_reserve->processKeys(
				[ ]( term::Window& hwnd, const std::string& str )
				{
					// clears the result area
					//
					hwnd.clear( );
					hwnd.move( 0, 3 );
					
					INS_INFO info{ };
					if ( !insparse::parseInstruction( hwnd, str, info ) )
						return true;
					
					processCommand( hwnd, info );
					return info.type != EXIT;
				} );
	}
}