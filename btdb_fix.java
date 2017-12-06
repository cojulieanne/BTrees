import java.util.*;
import java.io.*;
public class btdb_fix {
	//java -Xmx32M btdb Data.bt Data.values
	
	//global variables initialization
	
	//Files to open
	public static String File_bt= "Data.bt"; // set as Data.bt by default --> contains keys,offsets, nodes
	public static String File_values = "Data.values"; // contains values of keys
	
	//variables for read and write + commands
	public static int m=7; //this is changeable depending on the degree of bt the user prefers
	public static final int length = 3*m-1; //fixed bytes for writing 
	public static final String CMD_INSERT = "insert", CMD_UPDATE= "update", CMD_SELECT = "select",CMD_DELETE = "delete",CMD_EXIT = "exit";
	
	// Data.values variables
	public static int value_recordCount = 0; // counter for # of keys
	public static final int value_recordBytes = 8; // recordCount when RAM written will only be limited to 8 bytes
	public static final int value_StringBytes = 258; //2 bytes length || 256 bytes string value
	
	//Data.bt variables
	public static int bt_recordCount = 0; // counter for # of nodes
	public static int bt_rootLocation = 0; // tracker for root location
	public static final int bt_recordBytes = 16;
	
	/** not sure what the next currentFocus, newfocus are for**/
	public static int currentFocus = 0;
	public static int Newfocus = -1;
	public static Input read;
	public static Scanner sc = new Scanner(System.in);

	public static ArrayList<int[]> Records = new ArrayList<int[]>(); //record of all array representation of nodes
	public static ArrayList<String> Values = new ArrayList<String>(); // record of all values
	public static int[] keyArray; //array in focus
	
	//generalized input Object
	public static class Input{
		String command;
		String value;
		int key;
		int offset;
		Input(String inp){
			value_recordCount++;
			offset = value_recordCount;
			String[] explode = inp.split(" ");
			this.command = explode[0];
			if(explode.length>1) this.key = Integer.valueOf(explode[1]);
			if (explode.length>2) this.value = String.join(" ", Arrays.copyOfRange(explode, 2, explode.length));
		}
	}
	
	public static void main(String[] args) throws IOException{
		/** dont forget to make a function for error handling **/
		
		File_bt = args[0];
		File_values = args[1];
		btdb_init();
		
		while(sc.hasNext()) {
			String inp = sc.nextLine();
			read = new Input(inp);
			
			/** figure out universal searchnode**/
			int ref_index = searchNode(bt_rootLocation,2);
			switch(read.command) {
				case CMD_INSERT:
					insert1(ref_index);
					break;
				case CMD_UPDATE:
					update(2);
					break;
				case CMD_SELECT:
					select(2);
					break;
				case CMD_DELETE:
					break;
				case CMD_EXIT:
					System.exit(0);
					break;
				default:
					System.out.println("ERROR: invalid command");
			}
			Records.set(currentFocus, keyArray);
			System.out.print(">");
		}
		
	}
	
	public static void btdb_init() {
		createNew();
		keyArray = Records.get(bt_rootLocation);
		System.out.println(">");
	}
	
	// method for adding a new bt array with all elements equal to -1; call this for splitting and promoting
	public static void createNew() {
		int[] newRecords = new int[length];
		Arrays.fill(newRecords, new Integer(-1));
		Records.add(newRecords);
		bt_recordCount +=1;
	}
	
	public static boolean exist(){
		for(int i = 2; i < length; i = i+3){
			int keyTemp = keyArray[i];			
			if(keyTemp == read.key){
				return true;			
			}
		}
		return false;
	}
	
	public static void insert1(int index) {
		if(keyArray[index]==-1) {
			keyArray[index-1] = -1;
			keyArray[index] = read.key;
			keyArray[index+1] = read.offset;
		}
		else if(keyArray[index]==read.key) System.out.printf("< ERROR: %d already exists. \n", read.key);
		else {
			if(keyArray[length-3]!=-1) return; //split 
			int[] bt = {-1, read.key, read.offset};
			move(index, bt);
		}
	}
	
	public static void move(int index, int[] bt) {
		if(bt[1]==-1) return;
		else {
			if(index==length) return; //split
			else {
				 int[] temp = {keyArray[index-1], keyArray[index], keyArray[index+1]};
				 keyArray[index-1]=bt[0];
				 keyArray[index]=bt[1];
				 keyArray[index+1]=bt[2];
				 move(index+=3, temp);
			}
		}
		
	}
	
	public static int findPromote(int index) {
		int order = (index+1)/2;
		if(m%2==1) {
			int low_mid = (m/2)*3-1;
			int high_mid = low_mid+3;
			if(index<low_mid) return low_mid;
			else if(index<high_mid) return index;
			else return high_mid;
		}
		else {
			int mid = ((m+1)/2)*3-1;
			int nextmid = mid+3;
			if(order<mid) return mid;
			else if (order>nextmid) return nextmid;
			else return index;
		}
	}
	
	
	public static void split1(int index) {
		createNew();
		// not yet done
	}
	
	public static void split(int key, int promote){			
		//SPLIT CURRENT ARRAY TO 2
		int[] parent_array = new int[length]; 
		int[] temp = keyArray; //current focus array		
		int index = 2;	
		int promote_value = 0;//offset_value
		//create new child
		createNew();
		keyArray = Records.get(bt_recordCount-1);	//newly created child
		
		//update current array and new array
		for(int i = 2; i < length; i = i+3){ //loop in temp	
			if (temp[i] == promote){
				promote_value = temp[i+1];
				temp[i] = -1;
				temp[i+1] = -1;
				temp[i+2] = -1;
			}
			else if(temp[i] > promote){
				keyArray[index] = temp[i];
				keyArray[index+1] = temp[i+1];
				//keyArray[index+2] = temp[i+2]; //no need for child reference
				
				temp[i] = -1;
				temp[i+1] = -1;
				temp[i+2] = -1;
				index+=3;
			}
		}	
		Newfocus = bt_recordCount-1;  //int Newfocus = bt_recordCount-1; //focus of new child	
		
		//PARENT_ARRAY
		parent_array = Records.get(bt_rootLocation);				
		if(temp[0] == -1 && keyArray[0] == -1){ //if no parent and root, create new root
			createNew(); //create new parent/root
			bt_rootLocation = bt_recordCount-1; //new root location
			
			//Assign parent node to children
			keyArray[0] = bt_rootLocation; 	
			temp[0] = bt_rootLocation;
		
			//update records
			Records.set(currentFocus, temp);			
			Records.set(Newfocus, keyArray);
			
			parent_array = Records.get(bt_rootLocation);
			//Assign children to parent
			parent_array[1] = currentFocus;
			parent_array[1+3] = Newfocus;
			Records.set(bt_rootLocation, parent_array);
		}
		
		if(temp[0] != -1){ //already with existing parent			
			if(parent_array[13] != -1){ //full
				System.out.print(parent_array[13] + " FULL");
			}
			else{ //if not full
				keyArray[0] = bt_rootLocation;				
			}	
		}		
		//add promoted nodes if not same as key
		if(key != promote){
			//insert to root node			
			promote_to_root(promote, promote_value,parent_array,Newfocus);		
			Records.set(bt_rootLocation, parent_array);	
		}
			
	}
	
	public static void promote_to_root(int key, int offset_value, int[] parent_array, int Newfocus){
		//keyArray = keyArray[i]
		for(int i = 2; i < length; i = i+3){
			int keyTemp = parent_array[i];
			if(keyTemp == -1){ 							//if empty space
				parent_array[i] = key; 						//insert key
				parent_array[i+1] = offset_value; 			//insert offset of value
				parent_array[i+2] = Newfocus;		//new child offset
				break;
			}
			else{
				if (key < keyTemp){						
					for(int j =  length - 6; j >= i; j = j-3){
						if (parent_array[j] != -1){							
							parent_array[j+3] = parent_array[j];		//insert key
							parent_array[j+3+1] = parent_array[j+1];	//insert offset of value
							parent_array[j+3+2] = parent_array[j+2];	//child offset
						}
					}
					parent_array[i] = key; 						//insert key
					parent_array[i+1] = offset_value; 		//insert offset of value
					parent_array[i+2] = Newfocus;	//new child offset
					break;
				}					
			}
		}		
	}
	
	
	public static void insert() throws IOException{	
		for(int i = 2; i < length; i = i+3){
			int keyTemp = keyArray[i];
			if(keyTemp == -1){ 							//if empty space
				keyArray[i] = read.key; 						//insert key
				keyArray[i+1] = value_recordCount; 		//insert offset of value
				keyArray[i+2] = Newfocus;
				break;
			}
			else{
				if (read.key < keyTemp){						
					for(int j =  length - 6; j >= i; j = j-3){
						if (keyArray[j] != -1){							
							keyArray[j+3] = keyArray[j];		//insert key
							keyArray[j+3+1] = keyArray[j+1];	//insert offset of value
							keyArray[j+3+2] = keyArray[j+2];
						}
					}
					keyArray[i] = read.key; 						//insert key
					keyArray[i+1] = value_recordCount; 		//insert offset of value
					keyArray[i+2] = Newfocus;
					break;
				}					
			}
		}
		Values.add(read.value);  //add value to value array	
		write();
		value_recordCount += 1;			
		System.out.printf("< %d inserted.\n", read.key);				
	}
	
	public static void write() throws IOException {		
		//Write in Data.bt
		RandomAccessFile bt = new RandomAccessFile(File_bt, "rwd");
		bt.writeLong(bt_recordCount+1); //write/update num of records		
		bt.writeLong(bt_rootLocation); //ROOT		
		bt.seek(bt_recordBytes + bt_recordCount * length); 		
		
		
		for(int i = 0; i < length ; ++i){
			bt.writeInt(keyArray[i]); 			
		}
			
		for(int[] recordnum : Records){
			for(int x : recordnum){
				System.out.printf("%d ", x);
			}
			System.out.println();
		}
		System.out.println();
		bt.close();
		
		//Write in Data.values
		RandomAccessFile values = new RandomAccessFile(File_values, "rwd");		
		values.writeLong(value_recordCount+1); //write/update num of records
		//loop to update all records
		values.seek(value_recordBytes + value_recordCount * value_StringBytes); //look which "record" to updated/add new line
		values.writeShort(read.value.length()); 	//write length of value
		values.write((read.value).getBytes("UTF8")); 	//write value after converting to bytes
		values.close();	
	}	
	
	public static void update(int index) {
		//check if key already exists (error if it does not)
		if(index==length || keyArray[index]>read.key) {
			System.out.printf("< ERROR: %d does not exist. \n", read.key);
		}
		else if(keyArray[index]==read.key) {
			Values.set(keyArray[index+1], read.value);
		}
		else {
			update(index+=2);
		}
	}
	
	public static void select(int index){
		//using key, look for which record the value is in
		if(index == length) System.out.println("ERROR: "+ read.key + " does not exist.");
		else {
			if(keyArray[index]== read.key) System.out.println(Values.get(keyArray[index+1]));
			else select(index+-3);
		}
	}
	
	public static int searchNode(int focus, int index) {
		keyArray = Records.get(focus);
		int temp_key = keyArray[index];
		int temp_child = keyArray[index-1];
		if(index==length) {
			if(temp_child==-1) return index;
			else return searchNode(temp_child,2);
		}
		else {
			if(temp_key==-1) {
				if(temp_child==-1) return index;
				else return searchNode(temp_child,2);
			}
			else if (temp_key==read.key) return index;
			else if (temp_key< read.key) return searchNode(focus, index+=3);
			else {
				if (temp_child==-1) return index;
				else return searchNode(keyArray[index-1],2);
			}
		}
	}
	
}
