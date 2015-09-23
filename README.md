## Input File Formats

+ **Query file**: Each line contains a query. A query contains the following comma-separated values:
	* Id: Integer
	* X Coordinate: Double
	* Y Coordinate: Double
	* Query keywords: A series of integers

	Example: 1,5.2,3.5,4,5,9

+ **Location file**: Data file containing the a spatial location in each line. Each location contains the following comma-seperated values:
	* Id: Integer
	* X Coordinate: Double
	* Y Coordinate: Double

+ **Text File**: Contains the keywords corresponding to the locations in location file. The keywords are stored in a B-tree.

## Creating the Text File

The keywords corresponding to each spatio-textual object is stored in the *text file* in the form of a B-tree. ```build.StoreDocument``` class is used to create this *text file*. The input file of this class should contain entries of the following format in each line:

	Id, wordId1 weight1, wordId2 weight2, ....

Where wordId should be an integer and weight should be a double.

If you have a file of the following format

	Id, wordId1, wordId2, ....

You can calculate the weights using the `WeightCompute` class.