//Alexander Len
//December 15, 2018
//CS 149 OS
//Fabio di Troia

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Project {
	
	File[] processes;
	AddressSpace[] spaces;
	boolean shared = true; //By default, processes using the same program will be shared
	
	public Project (String[] files) {
		this.processes = new File[3];
		this.spaces = new AddressSpace[3];
		try {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				File file = new File(fileName);
				processes[i] = file;
				//Iterate through processes to see if another process is using the same program
				//point to the same address space if there is
				boolean duplicate = false;
				for (int j = 0; j < i; j++) {
					if (processes[j].getName() == file.getName()) {
						spaces[i] = spaces[j];
						duplicate = true;
						break;
					}
				}
				//if no other process is using the same program create a new address space
				if (duplicate != true) {
					ArrayList<String> text = readFile(file);
					AddressSpace space = parseFileText(text);
					spaces[i] = space;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> readFile(File file) throws Exception{
		FileReader fr;
		fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);

		ArrayList<String> text = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			if (!line.equals("")) {
				text.add(line);
			}
		}
		br.close();
		fr.close();
		return text;
	}
	
	public AddressSpace parseFileText(ArrayList<String> text) throws Exception {
		HashMap<String, Object[]> regions = new HashMap<String, Object[]>();
		for (String s : text) {
			//Split into 2 parts, region name and arguments
			String[] seperate = s.split(":");
			//Split args by commas
			String[] splitArgs = seperate[1].split(",");
			//remove spaces from args
			for (String arg : splitArgs) {
				arg.replaceAll(" ", "");
			}
			Object[] objects = new Object[splitArgs.length];
			//parse args
			for (int i = 0; i < splitArgs.length; i++) {
				String firstLetter = splitArgs[i].substring(0, 1);
				if (firstLetter.equals("\"")){
					//It is a String
					objects[i] = splitArgs[i];
				} else if (firstLetter.equals("'")) {
					//It is a char
					//get the char within the ' '
					objects[i] = splitArgs[i].charAt(1);
				} else {
			        try
			        { 
			        	//Check if integer
			        	Integer num = Integer.parseInt(splitArgs[i]);
			        	objects[i] = num;
			        }  
			        catch (NumberFormatException notInt)  
			        { 
			        	//Not an integer
			            try
			            { 
			                //Check if float
			                Float num = Float.parseFloat(splitArgs[i]);
			                objects[i] = num;
			            }  
			            catch (NumberFormatException notFloat) 
			            { 
			            	//Not a floating point number
			            	//Defaults to String
			            	objects[i] = splitArgs[i];
			            } 
			        }
				}
			}
			//Key is region name, objects is array of all data
			regions.put(seperate[0], objects);
		}
		AddressSpace space = new AddressSpace(regions.get("Stack"), regions.get("Dynamic"), regions.get("BSS"), regions.get("Data"), regions.get("Text"));
		return space;
	}
	
	class AddressSpace{
		//Address 0 starts at Text
		//Addresses go from 0-50
		//For this, I gave each 10 addresses
		int stackStartAddress = 50;
		int dynamicStartAddress = 30;
		int bssStartAddress = 20;
		int dataStartAddress = 10;
		int textStartAddress = 0;
		HashMap<Integer, Object> stack;
		HashMap<Integer, Object> dynamic;
		HashMap<Integer, Object> bss;
		HashMap<Integer, Object> data;
		HashMap<Integer, Object> text;
		
		//Helper function to copy values into the actual address space and assign addresses
		private void copyIntoHashMap(HashMap<Integer, Object> map, Object[] array, int startAddress) {
			int address = startAddress + map.size();
			for(int i = 0; i < array.length; i++) {
				map.put(address, array[i]);
				address++;
			}
		}
		
		//Stack needs its own function as it grows downward
		//Starts from 50 and goes down
		private void copyIntoStack(Object[] array) {
			int address = stackStartAddress - stack.size();
			for(int i = 0; i < array.length; i++) {
				stack.put(address, array[i]);
				address--;
			}
		}
		
		//ctor
		public AddressSpace(Object[] a, Object[] b, Object[] c, Object[] d, Object[] e) {
			stack = new HashMap<Integer, Object>();
			dynamic = new HashMap<Integer, Object>();
			bss = new HashMap<Integer, Object>();
			data = new HashMap<Integer, Object>();
			text = new HashMap<Integer, Object>();
			copyIntoStack(a);
			copyIntoHashMap(dynamic, b, dynamicStartAddress);
			copyIntoHashMap(bss, c, bssStartAddress);
			copyIntoHashMap(data, d, dataStartAddress);
			copyIntoHashMap(text, e, textStartAddress);
		}
		
		//Checks if the address is valid
		public void getObjectAtAddress(int address) {
			if (address < 0 || address > stackStartAddress) {
				sop("Invalid Address: " + address);
			} else if (address < dataStartAddress){
				getObject(text, address);
			} else if (address < bssStartAddress) {
				getObject(data, address);
			} else if (address < dynamicStartAddress) {
				getObject(bss, address);
			} else if (address < dynamicStartAddress + dynamic.size()) {
				getObject(dynamic, address);
			} else if (address > stackStartAddress - stack.size()) {
				getObject(stack, address);
			} else {
				sop("Object could not be found: " + address);
			}
		}
		
		//Actually checks if something is at the address and prints it
		private void getObject(HashMap<Integer, Object> map, int address) {
			Object obj = map.get(address);
			if (obj == null) {
				sop("Object could not be found: " + address);
			} else {
				sop("Object Found, Address: " + address + ", Value: " + obj);
			}
		}
		
		//Just to make print shorter to type
		private void sop(Object obj) {
			System.out.println(obj);
		}
	}
	
	/* 
	Files should be a format such the one below
	Region names are Case-sensitive
	
	Stack: 30, -5, 'a', 3.6, "ciao"
	Dynamic: 900, 50, 10
	BSS: 30, -5, 'a', 3.6, "ciao"
	Data: 30, -5, 'a', 3.6, "ciao"
	Text: \x10, \xa4, \x30
	
	 */
	
	public static void main(String args[]) {
		//input should be in the format of (including spaces)
		//programA.txt programB.txt programA.txt
		//where there would be full addresses for the text files
		
		//removes spaces
		for (String s : args) {
			s.replaceAll(" ", "");
		}
		String[] files = {args[0], args[1], args[2]};
		Project project = new Project(files);
		project.printProject();
	}
	
	public void printProject() {
		for (int i = 0; i < processes.length; i++) {
			System.out.println("---");
			System.out.println("process " + (i+1) + " runs \"" + processes[i].getName() + "\"");
			System.out.println("");
			System.out.println("Address Space");
			System.out.println("");
			System.out.println("Stack: " + spaces[i].stack);
			System.out.println("Dynamic: " + spaces[i].dynamic);
			System.out.println("BSS: " + spaces[i].bss);
			System.out.println("Data: " + spaces[i].data);
			System.out.println("Text: " + spaces[i].text);
			ArrayList<String> shared = new ArrayList<String>();
			for (int j = 0; j < processes.length; j++) {
				if (j != i && processes[j].getName().equals(processes[i].getName())) {
					shared.add(" process " + (j+1) + ",");
				}
			}
			System.out.println("");
			if (shared.isEmpty()) {
				System.out.println("Address Space is Not Shared");
			} else {
				System.out.println("Address Space is Shared With: " + shared);
			}
		}
	}
}
