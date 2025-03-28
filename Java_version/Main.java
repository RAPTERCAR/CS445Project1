
public class Main extends Thread {
    //collection of final variables
    public static final int TOTAL_BLOCKS = 512;
    public static final int MAX_FILES = 100;
    public static final int MAX_OPEN_FILES = 100;
    public static final int MAX_PROCESS_FILES = 50;
    public static final int BLOCK_SIZE = 2048;
    //instances of block arrays
    static Object[] disk = new Object[TOTAL_BLOCKS];
    static Vol_Control_Block vcb = new Vol_Control_Block();
    static Directory_Entry[] directory = new Directory_Entry[MAX_FILES];
    static Sys_Open_File_Table[] sys_open_file_table = new Sys_Open_File_Table[MAX_OPEN_FILES];
    static Process_Open_File_Table[] process_open_file_table = new Process_Open_File_Table[MAX_PROCESS_FILES];
    
    public static void main(String[] args){
        test_setup();
        disk[0] = vcb;

    }
    //method that makes sure the setup is working
    static void test_setup(){
        System.out.println("Testing File System Setup...");
        //print vcb details
        System.out.printf("Number of blocks: %d\n", vcb.num_of_blocks);
        System.out.printf("Block size: %d bytes\n", vcb.get_free_count());
        System.out.printf("Free block count: %d\n", vcb.free_count);

    }

    static void create(String name, int size, String data){
        System.out.println("aaa");

    }
    static void write(String data, int size){
        System.out.println("aaa");
        int index = find_free(size);

    }
    //splits data string into substrings in an array to be placed into blocks
    static String[] split_data(String data){
        //ArrayList split = new ArrayList<String>();
        int size = (int)Math.ceil(data.length() / BLOCK_SIZE);
        String[] split = new String[size];
        int end = 2048;
        int start = 0;
        for(int i = 0; i < size; i++){
            split[i] = data.substring(start, end);
            start = end + 1;
            end += 2048;
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

    public Data_Block(String s){
        data = s.toCharArray();
    }

    public void modify_data(String s){
        data = s.toCharArray();
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

    public Sys_Open_File_Table(String name, File_Control_Block f){
        file_name = name.toCharArray();
        fcb = f;
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
}