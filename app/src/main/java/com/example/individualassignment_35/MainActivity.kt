package com.example.individualassignment_35

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.individualassignment_35.ui.theme.IndividualAssignment_35Theme
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndividualAssignment_35Theme {
                makeScreen(makeInitialBoard())
            }
        }
    }
}

//function to make a random starting board configuration in the format
// of the whole top row being random numbers.
fun makeInitialBoard(): List<Int> {
    var ret = (1..9).shuffled().take(9)
    return ret
}

//function to return a 'row' from the array representing a sub-grid.
//Returns an array of nulls if values outside of 0..2 are given
fun getSubRow(sgrid: Array<MutableState<Cell>>, rowInd: Int): Array<Int> {
    var returnRow = Array<Int>(3){ 0 }
    if(!(rowInd in 0..2)) {
        return returnRow
    }
    for(i in 0..2){
        returnRow[i] = sgrid[(rowInd*3) + i].value.getV()
    }
    return returnRow
}
//function to return a 'column' from the array representing a sub-grid.
//Returns an array of nulls if values outside of 0..2 are given
fun getSubCol(sgrid: Array<MutableState<Cell>>, colInd: Int): Array<Int> {
    var returnCol = Array<Int>(3){ 0 }
    if(!(colInd in 0..2)) {
        return returnCol
    }
    for(i in 0..2){
        returnCol[i] = sgrid[(i*3) + colInd].value.getV()
    }
    return returnCol
}

//function to display a subgrid array
@Composable
fun displaySubGrid(sgrid: Array<MutableState<Cell>>){
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 120.dp)
            .border(width = 2.dp, color = Color(0XFF7E8080))
    ){
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            content = {
                items(sgrid.size) { itm ->
                    sgrid[itm].value.display()
                }
            }
        )
    }
}

//function to ensure sub-grid is correct and full
fun assertSubGrid(sgrid: Array<MutableState<Cell>>): Boolean {
    //extract the values of the sub grid
    val sgridValues = Array(sgrid.size) {i ->
        sgrid[i].value.getV()
    }
    //If there are all nine distinct numbers in the subgrid and 0 is not in the subgrid
    // then it is complete.
    return sgridValues.distinct().size == 9 && !(0 in sgridValues)
}
//function to ensure row/column is correct
fun assertRowCol(array1: Array<Int>, array2: Array<Int>, array3: Array<Int>): Boolean{
    val allValues = array1 + array2 + array3
    //If all nine distinct number are in the row/col and 0 is not in it, then it is complete.
    return allValues.distinct().size == 9 && !(0 in allValues)
}
//function to check correctness of the whole board
fun assertBoard(board: Array<Array<MutableState<Cell>>>): Boolean {
    //check all sub grids
    for(sgrid in board) {
        if (!assertSubGrid(sgrid)) return false
    }
    //check all rows
    for(sGridRow in 0..2) {
        for(row in 0..2) {
            if (!assertRowCol(
                getSubRow(board[sGridRow*3 + 0], row),
                getSubRow(board[sGridRow*3 + 1], row),
                getSubRow(board[sGridRow*3 + 2], row)
            )) return false
        }
    }
    //check all columns
    for(sGridCol in 0..2) {
        for(col in 0..2) {
            if(!assertRowCol(
                getSubCol(board[sGridCol + 0], col),
                getSubCol(board[sGridCol + 3], col),
                getSubCol(board[sGridCol + 6], col)
            )) return false
        }
    }
    return true
}


//A class linking a specific cell's composables to a value. This allows individual
// cells to be updated in real-time by making use of the cells' clickable attribute
class Cell(selected: MutableState<Cell?>,
           isMutable: Boolean = true,
           initial: Int = 0) {
    var v = mutableStateOf(initial)   //the value held by the cell
    var s = selected                  //the current selected cell mutable state
    val mutable = isMutable

    fun getV(): Int {
        return v.value
    }

    fun setV(newV: Int) {
        v.value = newV
    }

    @Composable
    fun display() {
        //text value of box is blank for value 0. Box Color is highlighted yellow
        // if this cell is selected. Box is always gray if given an initial value.
        val t = if (getV() != 0) getV().toString() else " "
        var boxColor = if(!mutable) Color.LightGray
            else if (s.value == this@Cell) Color(0xFFD4E1A6)
            else Color.White
        Box(
            modifier = Modifier
                .background(color = boxColor)
                .fillMaxSize()
                .border(width = 1.dp, color = Color(0XFFBBBBBB))
                .size(width = 33.dp, height = 40.dp)
                .clickable {
                    //When clicked, the cell either sets itself as the selected cell
                    // or, if it is already selected, unselects itself.
                    //Retains current selected cell if the current cell is not mutable.
                    s.value = if(!mutable) s.value else if (s.value == this@Cell) null else this@Cell
                }
        ) {
            Text(
                text = t,
                fontSize = 16.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

//The main function to create the sudoku and display the screen
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@Composable
fun makeScreen(initialBoard: List<Int>? = null){
    /*The board is represented as 9 arrays, each of which represents a sub-grid.
      The sub-grids and squares within are identified by indices:
      +--+--+--+
      | 0| 1| 2|
      +--+--+--+
      | 3| 4| 5|
      +--+--+--+
      | 6| 7| 8|
      +--+--+--+
     */

    //a mutable state representing the currently-selected cell to modify
    val selectedCell: MutableState<Cell?> = remember {mutableStateOf(null)}
    //the actual set of values in the grid
    val subGrids: Array<Array<MutableState<Cell>>>
    = remember { Array(9) { Array(9) { mutableStateOf( Cell(selectedCell) )}} }

    //set the initial immutable values
    var countSubGrid = 0
    var countCell = 0
    if (initialBoard != null) {
        for(value in initialBoard) {
            subGrids[countSubGrid][countCell] = remember { mutableStateOf(Cell(selectedCell, false, value ))}
            countCell = (countCell + 1) % 3
            countSubGrid += if(countCell == 0) 1 else 0
        }
    }

    //define snackbar host and scope
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {innerPadding ->

        //start by checking for completed puzzle
        if(assertBoard(subGrids)) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "You win!",
                    duration = SnackbarDuration.Indefinite
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
        ) {
            //Make a the grid for the puzzle based on the values in subGrids
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Fixed(3),
                content = {
                    items(subGrids.size) { ind ->
                        displaySubGrid(subGrids[ind])
                    }
                }
            )
            //Make a collection of buttons used to modify the cells of the puzzle
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(25.dp),
                content = {
                    val buttons = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
                    items(buttons.size) { ind ->
                        Button(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(6.dp),
                            onClick = {
                                selectedCell.value?.setV(buttons[ind])
                            }
                        ){
                            Text(
                                text = if(buttons[ind] != 0) buttons[ind].toString() else "X",
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            )
            //A button to clear the full board
            Button(
                onClick = {
                    for(sgrid in subGrids) {
                        for(cell in sgrid){
                            if(cell.value.mutable) cell.value.setV(0)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp)
            ) {
                Text(
                    text = "Clear Puzzle"
                )
            }
        }
    }
    assertBoard(subGrids)
}

@Preview(showBackground = true)
@Composable
fun preview() {
    IndividualAssignment_35Theme {
        makeScreen()
    }
}