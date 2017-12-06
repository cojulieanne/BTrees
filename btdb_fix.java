import java.util.*;
import java.io.*;
public class btdb_fix {
	//java -Xmx32M btdb Data.bt Data.values
	
	//global variables initialization
	
	//Files to open
	public static String File_bt= "Data.bt"; // set as Data.bt by default --> contains keys,offsets, nodes
	public static String File_values = "Data.values"; // contains values of keys
	
	//variables for read and write + commands
	public static int m=5; //this is changeable depending on the degree of bt the user prefers
	public static final int length = 3*m-1; //fixed bytes for writing 
	public static final String CMD_INSERT = "insert", CMD_UPDATE= "update", CMD_SELECT = "select",CMD_DELETE = "delete",CMD_EXIT = "exit";
	
	// Data.values variables
	public static int value_recordCount = 0; // counter for # of keys
	public static final int value_recordBytes = 8; // recordCount when RAM written will only be limited to 8 bytes
	public static final int value_StringBytes = 258; //2 bytes length || 256 bytes string value
	
	//Data.bt variables
	public static int bt_recordCount = -1; // counter for # of nodes
	public static int bt_rootLocation = 0; // tracker for root location
	public static final int bt_recordBytes = 16;
	
	/** not sure what the next currentFocus, newfocus are for**/
	public static int keyArray_index = 0;
	public static int destArray_index = 0;
	public static Input read;
	public static Scanner sc = new Scanner(System.in);

	public static ArrayList<int[]> Records = new ArrayList<int[]>(); //record of all array representation of nodes
	public static ArrayList<String> Values = new ArrayList<String>(); // record of all values
	public static int[] keyArray; //array in focus
	public static int[] dest_Array; //destination array
	
	//generalized input Object
	public static class Input{
		String command;
		String value;
		int key;
		int offset;
		Input(String inp){
			String[] explode = inp.split(" ");
			this.command = explode[0];
			int length = explode.length;
			if(length>1) this.key=Integer.valueOf(explode[1]);
			if(this.command.equals("insert")) {
				if(length>2) this.value = String.join(" ", Arrays.copyOfRange(explode, 2, explode.length));	
				else this.value="";
				Values.add(this.value);
				offset = value_recordCount;
				value_recordCount++;
			}
			if(this.command.equals("update")) {
				if(length>2) this.value = String.join(" ", Arrays.copyOfRange(explode, 2, explode.length));	
				else this.value="";
			}
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
			System.out.println(keyArray_index);
			System.out.println(ref_index);
			switch(read.command) {
				case CMD_INSERT:
					insert(ref_index);
					write();
					System.out.printf("< %d inserted.\n", read.key);	
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
			//Records.set(currentFocus, keyArray);
			System.out.print(">");
		}
		
	}
	
	public static void btdb_init() {
		createNew();
		keyArray = Records.get(bt_rootLocation);
		keyArray_index = bt_recordCount;
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
	
	public static void insert(int index) {
		if(keyArray[index]==-1) {
			keyArray[index-1] = -1;
			keyArray[index] = read.key;
			keyArray[index+1] = read.offset;
		}
		else if(keyArray[index]==read.key) System.out.printf("< ERROR: %d already exists. \n", read.key);
		else {
			if(keyArray[length-3]!=-1) {
				split(index);
				return;
			}
			int[] bt = {-1, read.key, read.offset};
			move_forward(index, bt, length);
		}
	}
	
	public static void move_forward(int index, int[] bt, int last) {
		if(bt[1]==-1 || index>last) return;
		else {
			 int[] temp = {keyArray[index-1], keyArray[index], keyArray[index+1]};
			 keyArray[index-1]=bt[0];
			 keyArray[index]=bt[1];
			 keyArray[index+1]=bt[2];
			 move_forward(index+=3, temp, last);
		}
	}
	
	public static void move_reverse(int index, int[] bt, int last) {
		if(index<last) return;
		else {
			 int[] temp = {keyArray[index-1], keyArray[index], keyArray[index+1]};
			 keyArray[index-1]=bt[0];
			 keyArray[index]=bt[1];
			 keyArray[index+1]=bt[2];
			 move_reverse(index-=3, temp, last);
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
	
	
	public static void split(int index) {
		createNew();
		int promote = findPromote(index);
		int[] promote_array = {keyArray[promote-1], keyArray[promote], keyArray[promote+1]};
		int[] bt = {-1, read.key, read.offset};
		if(index<promote) move_forward(index,bt , promote);
		else if(index>promote) move_reverse(index, bt, promote);
		dest_Array = Records.get(bt_recordCount);
		destArray_index = bt_recordCount;
		read.key = promote_array[1];
		read.offset = promote_array[2];
		System.out.println("promote " + read.key);
		System.out.println(Arrays.toString(keyArray));
		move_out(promote-1, 1);
		promote();
	}
	
	
	public static void promote() {
		if(keyArray[0]==-1) {
			createNew();
			bt_rootLocation = bt_recordCount;
			keyArray[0]= bt_recordCount;
			dest_Array[0]=bt_recordCount;
			keyArray = Records.get(bt_recordCount);
			insert(2);
			keyArray[1] = keyArray_index;
			System.out.println(destArray_index);
			keyArray[4] = destArray_index;
			keyArray_index = bt_recordCount;
		}
		else {
			keyArray = Records.get(keyArray[0]);
			
		}
	}
	
	public static void move_out(int index,int dest_index) {
		if(index>length-2) return;
		else {
			System.out.println("hdfsk");
			dest_Array[dest_index]=keyArray[index];
			keyArray[index] =-1;
			move_out(index+1, dest_index+1);
		}
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
		values.writeShort((read.value).length()); 	//write length of value
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
			System.out.println(Values.get(keyArray[index+1]));
		}
		else {
			update(index+=3);
		}
	}
	
	public static void select(int index){
		//using key, look for which record the value is in
		if(index == length) System.out.println("ERROR: "+ read.key + " does not exist.");
		else {
			if(keyArray[index]== read.key) System.out.println(Values.get(keyArray[index+1]));
			else select(index+=3);
		}
	}
	
	public static int searchNode(int focus, int index) {
		System.out.println(Arrays.toString(keyArray));
		int temp_child = keyArray[index-1];
		System.out.println("child "+ temp_child);
		System.out.println(focus + " " + index);
		if(index==length) {
			if(keyArray[index-1]==-1) return index-3;
			else return searchNode(temp_child, 2);
		}
		else {
			keyArray = Records.get(focus);
			keyArray_index = focus;
			int temp_key = keyArray[index];
			if(temp_key==-1) {
				if(temp_child==-1) {
					System.out.println(":skdjs");
					return index;
				}
				else {
					System.out.println("child");
					 return searchNode(temp_child,2);
				}
			}
			else if (temp_key==read.key) return index;
			else if (temp_key< read.key) return searchNode(focus, index+=3);
			else {
				if (temp_child==-1) return index;
				else return searchNode(temp_child,2);
			}
		}
	}

}
