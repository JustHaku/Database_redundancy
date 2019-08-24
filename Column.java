public class Column
{
	private String columnName;
	private String columnTypeNumber;
	private String columnDataType;
	private String columnSize;
	private String columnDataSize;
	private boolean columnIsNullable = true;
	private String columnAutoincrement;
	
	public Column(String name,String typeNumber,String dataType,String _columnSize,String isNullable){
		
		columnName = name;
		columnTypeNumber = typeNumber;
		columnDataType = dataType;
		columnSize = _columnSize;
		
		char nullable = isNullable.charAt(0);
		
        if(nullable == 'N'){
            columnIsNullable = false;
        }
	}
	
	public String getColumnName(){
		return columnName;
	}
	
	public String getDataType(){
		return columnDataType;
	}
	
	public int getColumnSize(){
		return Integer.valueOf(columnSize);
	}
	
	public int getTypeNumber(){
		return Integer.valueOf(columnTypeNumber);
	}
	
	public boolean getIsNullable(){
		return columnIsNullable;
	}
}