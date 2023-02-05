export const letterGen = ( ) => 
{
    const alphabet = "abcdefghijklmnopqrstuvwxyz";

    return alphabet[Math.floor( Math.random( ) * alphabet.length )];
};

export const numGen = ( min, max ) => 
{
    return Math.floor( Math.random( ) * ( max - min ) ) + min;
};

export const timeStrToSeconds = ( str ) =>
{
    let p = str.split(':'), s = 0, m = 1;

    while (p.length > 0)
    {
        s += m * parseInt(p.pop(), 10);
        m *= 60;
    }

    return s;
}

export const secondsToTimeStr = ( seconds ) =>
{
    let h = Math.floor( seconds / 3600 );
    let m = Math.floor( seconds % 3600 / 60 );
    let s = Math.floor( seconds % 3600 % 60 );

    return ( h > 9 ? h : h !== 0 ? '0' + h + ':' : '' ) + ( m > 9 ? m : '0' + m )  + ':' + ( s > 9 ? s : '0' + s );
};

// 0 - up | 1 - up-right | 2 - right | 3 - down-right | 4 - down | 5 - down-left | 6 - left | 7 - up-left
export const getIndexByDirection = ( direction, num_columns, num_rows, word_base_index, cur_letter_index ) => 
{
    let elem_row = Math.floor( word_base_index / num_columns ) + 1;
    let elem_column = word_base_index % num_columns + 1;

    // handle up/down direction
    switch ( direction )
    {
        case 0:
        case 1:
        case 7:
            elem_row -= cur_letter_index;
            break;
        case 3:
        case 4:
        case 5:
            elem_row += cur_letter_index;
            break;
        default:
            break;
    }

    // handle left/right direction
    switch ( direction )
    {
        case 1:
        case 2:
        case 3:
            elem_column += cur_letter_index;
            break;
        case 5:
        case 6:
        case 7:
            elem_column -= cur_letter_index;
            break;
        default:
            break;
    }

    if ( elem_column > num_columns || elem_column < 1 || elem_row > num_rows || elem_row < 1 )
        return -1;
    
    return elem_row * num_columns + elem_column - num_columns - 1;
};

export const getInfoByPoints = ( num_columns, num_rows, start_index, end_index ) => 
{
    let start_elem_row = Math.floor( start_index / num_columns ) + 1;
    let start_elem_column = start_index % num_columns + 1;

    let end_elem_row = Math.floor( end_index / num_columns ) + 1;
    let end_elem_column = end_index % num_columns + 1;

    let size_row = 0;
    let size_column = 0;
    let valid_directions = [ 0, 1, 2, 3, 4, 5, 6, 7 ];

    // no up or down, just sides
    if ( end_elem_row === start_elem_row )
        valid_directions = valid_directions.filter( v => ![0, 1, 7, 3, 4, 5].includes( v ) );

    // down
    else if ( end_elem_row - start_elem_row > 0 )
    {
        valid_directions = valid_directions.filter( v => ![0, 1, 7, 2, 6].includes( v ) );
        size_row = end_elem_row - start_elem_row;
    }

    // up
    else
    {
        valid_directions = valid_directions.filter( v => ![3, 4, 5, 2, 6].includes( v ) );
        size_row = start_elem_row - end_elem_row;
    }

    // no sides, just up or down
    if ( end_elem_column === start_elem_column )
        valid_directions = valid_directions.filter( v => ![1, 2, 3, 5, 6, 7].includes( v ) );
    
    // right
    else if ( end_elem_column - start_elem_column > 0 )
    {
        valid_directions = valid_directions.filter( v => ![5, 6, 7, 0, 4].includes( v ) );
        size_column = end_elem_column - start_elem_column;
    }
    
    // left
    else
    {
        valid_directions = valid_directions.filter( v => ![1, 2, 3, 0, 4].includes( v ) );
        size_column = start_elem_column - end_elem_column;
    }

    if ( size_column !== size_row && size_column && size_row )
        return [ -1, -1 ];

    return [ valid_directions[0], ( size_column ? size_column : size_row ) ];
};