cmake_minimum_required( VERSION 3.22 )
project( FS-2023 )

set( CMAKE_CXX_STANDARD 17 )
set( CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../binaries )

add_executable( ${PROJECT_NAME} main.cpp
				entity/animals/animal.cpp entity/animals/animal.h
				entity/animals/rabbit.cpp entity/animals/rabbit.h
				consts/consts.cpp consts/consts.h
				utils/utils.cpp utils/utils.h
				shared.h
				entity/foods/food.cpp entity/foods/food.h
				entity/animals/animals.h
				engine/engine.cpp engine/engine.h
				entity/animals/sheep.cpp entity/animals/sheep.h
				entity/animals/wolf.cpp entity/animals/wolf.h
				entity/animals/kangaroo.cpp entity/animals/kangaroo.h
				terminal/terminal.cpp terminal/terminal.h
				entity/entity.h entity/entity.cpp
				insparse/insparse.cpp insparse/insparse.h
				reserve/reserve.cpp reserve/reserve.h
				reserve/camera/camera.cpp reserve/camera/camera.h
				entity/foods/foods.h
				entity/foods/grass.cpp entity/foods/grass.h
				entity/foods/carrot.cpp entity/foods/carrot.h
				entity/foods/corpses.cpp entity/foods/corpses.h
				entity/foods/beef.cpp entity/foods/beef.h
				entity/animals/mistery.cpp entity/animals/mistery.h
				entity/foods/misteryfood.cpp entity/foods/misteryfood.h)

target_link_libraries( ${PROJECT_NAME} -lncurses )