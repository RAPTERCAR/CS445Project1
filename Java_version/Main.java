import java.util.concurrent.Semaphore;

public class Main extends Thread {
    //collection of final variables
    public static final int TOTAL_BLOCKS = 512;
    public static final int MAX_FILES = 100;
    public static final int MAX_OPEN_FILES = 100;
    public static final int MAX_PROCESS_FILES = 50;
    public static final int BLOCK_SIZE = 2048;
    private static final Semaphore tWrite = new Semaphore(1);
    private static final Semaphore tOpen = new Semaphore(1);
    private static final Semaphore tCreate = new Semaphore(1);
    //instances of block arrays
    static Data_Block[] disk = new Data_Block[TOTAL_BLOCKS];
    static Vol_Control_Block vcb = new Vol_Control_Block();
    static Directory_Entry[] directory = new Directory_Entry[MAX_FILES];
    static Sys_Open_File_Table[] sys_open_file_table = new Sys_Open_File_Table[MAX_OPEN_FILES];
    static ThreadLocal<Process_Open_File_Table[]> process_open_file_table = ThreadLocal.withInitial(() -> {
        Process_Open_File_Table[] array = new Process_Open_File_Table[MAX_PROCESS_FILES]; // Create an array of Process_Open_File_Table with size 5
        return array;
    });
    static int num_of_files = 0;
    static int sys_index = 0;
    static ThreadLocal<Integer> proc_index = ThreadLocal.withInitial(() -> 0);
    
    
    public static void main(String[] args){
        for (int i = 0; i < MAX_FILES; i++) {
            disk[i] = new Data_Block();
        }
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            sys_open_file_table[i] = new Sys_Open_File_Table();
        }
        Thread p1 = new Thread1();
        p1.start();

        while (p1.isAlive()) { 
            System.out.print("");
        }
        Thread p2 = new Thread2();
        Thread p3 = new Thread3();

        p2.start();
        p3.start();
    }

    static void create(String name, int size, String data){
        try{
            int index = find_free(size);
            tCreate.acquire();
            directory[num_of_files++] = new Directory_Entry(name, index, size);
            
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        finally{
            tCreate.release();
            open(name);
        }
    }
    static void write(String data, String name){
        try {
            int[] info = find_file(name);
            int index = info[1];
            int size = info[2];
            if(info[0] != -1){
                String[] datas = split_data(data, size);
                //if(datas.length < bigger){
                //    bigger = datas.length;
                //}
                tWrite.acquire();
                for(int i = 0; i < size; i++){
                    disk[index] = new Data_Block(datas[i]);
                    vcb.bit_map[index] = 1;
                    index++;
                }
                tWrite.release();
                System.out.println("Write successfull");
            }
            else{
                System.out.println("No such file found");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }
    static void open(String name){
        try {
            int[] info = find_file(name);
            Process_Open_File_Table[] proc = process_open_file_table.get();
            Integer pI = proc_index.get();

            if(info[0] != -1){
                tOpen.acquire();
                int[] sys = findSys(name);
                if (sys[1] == -1) {//if file isnt in open file table
                 sys_open_file_table[sys_index] = new Sys_Open_File_Table(name, new File_Control_Block(info[2],(Data_Block)disk[info[1]]));
                 sys[0] = sys_index++;
                }
                else{//if it is in the table
                    sys_open_file_table[sys[0]].mod_instance(1);
                }
                proc[pI++] = new Process_Open_File_Table(name,sys[0]);
                tOpen.release();
            }
            else{
                System.out.println("No such file found");
            }
            process_open_file_table.set(proc);
            proc_index.set(pI);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }

    static void close(String name) {
        try {
            int[] info = find_file(name);
            Process_Open_File_Table[] proc = process_open_file_table.get();
            //Integer pI = proc_index.get();
            if(info[0] != -1){//ensures file exists
                tOpen.acquire();
                int[] sys = findSys(name);
                if (sys[1] == 1) {//if file is on its last instance
                    sys_open_file_table[sys[0]] = new Sys_Open_File_Table(); //remove entry by replacing it with an empty entry
                }
                else if (sys[1] > 1){//if multiple instances, reduce by one
                    sys_open_file_table[sys[0]].mod_instance(-1);
                }
                else{
                    System.out.println("File not found");
                }
                tOpen.release();
                int pID = findProc(name);
                proc[pID] = new Process_Open_File_Table();//remove file from proc open file table by setting it to an empty object
                process_open_file_table.set(proc);
                System.out.println(name + " successfully closed");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static String read(String name) {
        StringBuilder content = new StringBuilder();
        int[] info = find_file(name);
        int index = info[1];
        Data_Block temp;
        if (info[0] != -1) {
            for(int i = 0; i < info[2]; i++){
                temp = (Data_Block)disk[index];
                content.append(temp.getData());
                index++;
            }
        }
        return content.toString();
    }
    //finds file in directory and returns array containing start and size, index 0 used to communicate if file found
    static int[] find_file(String name) {
        int[] contents = new int[3];
        contents[0] = -1; // -1 indicates no found file
    
        for (Directory_Entry d : directory) {
            if (d != null && new String(d.get_name()).trim().equals(name)) { 
                contents[0] = 1; // Found file
                contents[1] = d.get_start();
                contents[2] = d.get_size();
                return contents;
            }
        }
        return contents;
    }
    
    //splits data string into substrings in an array to be placed into blocks
    static String[] split_data(String data,int size){
        //ArrayList split = new ArrayList<String>();
        String[] split = new String[size];
        int end;
        String temp = data;
        boolean empty = false;//tracks if there is still data to split into blocks
        for(int i = 0; i < size; i++){
            if(!empty){
                end = 2048;
                if (temp.length() < 2048){
                    end = temp.length();
                    empty = true;
                }
                split[i] = temp.substring(0, end);
                temp= temp.substring(end);
            }
           else{//if no more data, fill add empty string 
                split[i] = "";
           }
           
        }
        for (int i = 0; i < size; i++) {
            System.out.println(split[i]);
        }
        return split;
    }
    //finds free space on disk and returns its starting index
    static int find_free(int size){
        if (vcb.free_count < size){
            return -1;
        }
        int start = -1;
        boolean free = false;
        int searchSize = 0;
        for (int i = 1; i < TOTAL_BLOCKS; i++){
            
            if (vcb.bit_map[i] == 0 && !free){
                start = i;
                searchSize = 1;
                free = true;
            }
            else if (vcb.bit_map[i] == 0 && free){
                searchSize++;
            }
            else if (vcb.bit_map[i] == 1){
                searchSize = 0;
                free = false;
                start = -1;
            }
            if(searchSize == size){
                break;
            }
        }
        return start;
    }
    //method that looks through system open file table to find instances
    static int[] findSys(String name) {
        int[] info = {-1,-1}; //index 0 represents file index on table, index 1 reps num of instances
        for (int i = 0; i < MAX_OPEN_FILES;i++ ) {
            if (sys_open_file_table[i].getName().equals(name)) {
                info[1] = sys_open_file_table[i].getInstance();
                info[0] = i;
                return info;
            }
        }
        return info;
    }
    //returns index of file in process open file table
    static int findProc(String name) {
        int info = -1; //index 0 represents file index on table, index 1 reps num of instances
        Process_Open_File_Table[] proc = process_open_file_table.get();
        for (int i = 0; i < MAX_OPEN_FILES;i++ ) {
            if (proc[i].getName().equals(name)) {
                info = i;
                return info;
            }
        }
        return info;
    }

    static void processFileSetup() {
        Process_Open_File_Table[] proc = process_open_file_table.get();
        for (int i = 0; i < MAX_PROCESS_FILES; i++) {
            proc[i] = new Process_Open_File_Table();
        }
        process_open_file_table.set(proc);
    }
}

class Thread1 extends Thread{
    @Override
    public void run() {
        p1Seq();
    }

    void p1Seq() {
        Main.processFileSetup();

        Main.create("file1", 4, "test");
        Main.write("this is file1", "file1");
        Main.close("file1");

        Main.create("file2", 4, "test2");
        Main.write("this is file2", "file2");
        Main.close("file2");
        
    }
}

class Thread2 extends Thread{
    @Override
    public void run() {
        p2Seq();
    }

    void p2Seq() {
        Main.processFileSetup();

        System.out.println("p2 started");
        Main.open("file1");
        System.out.println(Main.read("file1"));
        Main.close("file1");
    }
}

class Thread3 extends Thread{
    @Override
    public void run() {
        p3Seq();
    }

    void p3Seq() {
        Main.processFileSetup();

        System.out.println("p3 started");
        Main.open("file2");
        System.out.println(Main.read("file2"));
        Main.close("file2");
    }
}


class Vol_Control_Block{
    public int num_of_blocks;
    public int block_size;
    public int free_count;
    public int[] bit_map = new int[Main.TOTAL_BLOCKS];

    public Vol_Control_Block(){
        num_of_blocks = Main.TOTAL_BLOCKS;
        block_size= Main.BLOCK_SIZE;
        free_count = Main.TOTAL_BLOCKS;
        bit_map[0] = 1;
        for(int i = 1; i < Main.TOTAL_BLOCKS; i++){
            bit_map[i] = 0;
        }
    }

    public int get_free_count(){
        return free_count;
    }

    public void mod_free_count(int change){
        free_count = free_count + change;
    }
}

class Data_Block{
    public char[] data = new char[Main.BLOCK_SIZE];
    public Data_Block() {
        data = "".toCharArray();
    }

    public Data_Block(String s){
        data = s.toCharArray();
    }

    public void modify_data(String s){
        data = s.toCharArray();
    }

    public String getData() {
        return String.copyValueOf(data);
    }
}

class File_Control_Block{
    int size;
    Data_Block first_block;

    public File_Control_Block(int s, Data_Block f){
        size = s;
        first_block = f;
    }
}

class Process_Open_File_Table{
    char[] file_name = new char[20];
    int handle;

    public Process_Open_File_Table(){
        file_name = "".toCharArray();
        handle = -1;
    }
    public Process_Open_File_Table(String name, int i){
        file_name = name.toCharArray();
        handle = i;
    }
    public String getName() {
        return String.copyValueOf(file_name);
    }
}

class Sys_Open_File_Table{
    char[] file_name = new char[20];
    File_Control_Block fcb;
    int instances;

    public Sys_Open_File_Table() {
        file_name = "".toCharArray();
    }

    public Sys_Open_File_Table(String name, File_Control_Block f){
        file_name = name.toCharArray();
        fcb = f;
        instances = 1;
    }

    public void mod_instance(int i) {
        instances += i;
    }

    public int getInstance() {
        return instances;
    }

    public String getName() {
        return String.copyValueOf(file_name);
    }
}
class Directory_Entry {
    char[] file_name = new char[20];
    int start_block;
    int file_size;

    public Directory_Entry(String name, int start, int size){
        file_name = name.toCharArray();
        start_block = start;
        file_size = size;
    }
    public char[] get_name(){
        return file_name;
    }
    public int get_start(){
        return start_block;
    }
    public int get_size(){
        return file_size;
    }
}