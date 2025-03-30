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
        //disk[0] = vcb;
        create("testFile", 2, "testing create");
        write("testing write", "testFile");
        System.out.println(read("testFile"));
    }

    static void create(String name, int size, String data){
        try{
            System.out.println("Creating File");
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
            System.out.println("Attempting to Write to file");
            int[] info = find_file(name);
            int index = info[1];
            int bigger = info[2];
            if(info[0] != -1){
                String[] datas = split_data(data);
                if(datas.length < bigger){
                    bigger = datas.length;
                }
                for(int i = 0; i < bigger; i++){
                    tWrite.acquire();
                    disk[index] = new Data_Block(datas[i]);
                    tWrite.release();
                    index++;
                }
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
                if (findSys(name) == -1) {
                 sys_open_file_table[sys_index++] = new Sys_Open_File_Table(name, new File_Control_Block(info[2],(Data_Block)disk[info[1]]));
                }
                proc[pI++] = new Process_Open_File_Table(name,sys_index-1);
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
    static String[] split_data(String data){
        //ArrayList split = new ArrayList<String>();
        int size = (int)Math.ceil((data.length() / BLOCK_SIZE));
        if(size == 0){
            size = 1;
        }
        System.out.println(size);
        String[] split = new String[size];
        int end = data.length();
        int start = 0;
        for(int i = 0; i < size; i++){
            split[i] = data.substring(start, end);
            start = end + 1;
            end += 2048;
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
    static int findSys(String name) {
        int i = -1;
        for (Sys_Open_File_Table file : sys_open_file_table) {
            if (file.getName().equals(name)) {
                return i = file.getInstance();
            }
        }
        return i;
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

    public Process_Open_File_Table(String name, int i){
        file_name = name.toCharArray();
        handle = i;
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
        instances = 0;
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