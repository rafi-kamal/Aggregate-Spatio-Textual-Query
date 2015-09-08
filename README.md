## Input File Formats

+ Query file: Each line contains a query. A query contains the following comma-separated values:
	* Id: Integer
	* X Coordinate: Double
	* Y Coordinate: Double
	* Query keywords: A series of integers

	Example: 1, 5.2, 3.5, 4, 5, 9

+ Location file: Data file containing the a spatial location in each line. Each location contains the following comma-seperated values:
	* Id: Integer
	* X Coordinate: Double
	* Y Coordinate: Double

+ Text File: Contains the keywords corresponding to the locations in location file. The keywords are stored in a B-tree.