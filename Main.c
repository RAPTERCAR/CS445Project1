#include<pthread.h>
#include<stdio.h>
#include <string.h>


#define TOTAL_BLOCKS 512
#define MAX_FILES 100
#define MAX_OPEN_FILES 100
#define MAX_PROCESS_FILES 50
#define BLOCK_SIZE 2048

struct Vol_Control_Block {
    int num_of_blocks;
    int block_size;
    int free_count;
    int bit_map[TOTAL_BLOCKS];
};

struct Directory_Entry {
    char file_name[20];
    int start_block;
    int file_size;
};

struct File_Control_Block{
    int size;
    int* first_block;
};

struct Sys_Open_File_Table{
    char file_name[20];
    struct File_Control_Block fcb;
};

struct Process_open_File_Table{
    char file_name[20];
    int handle;
};

struct Data_Block{
    int block[BLOCK_SIZE];
};
// Instances:
int disk[TOTAL_BLOCKS];
struct Vol_Control_Block vcb;
struct Directory_Entry directory[MAX_FILES];
struct Sys_Open_File_Table sys_open_file_table[MAX_OPEN_FILES];
struct Process_open_File_Table process_open_file_table[MAX_PROCESS_FILES];

void initialize_vcb() {
    vcb.num_of_blocks = TOTAL_BLOCKS;
    vcb.block_size = BLOCK_SIZE;
    vcb.free_count = TOTAL_BLOCKS - 1; // Block 0 reserved
    for (int i = 0; i < TOTAL_BLOCKS; i++) {
        vcb.bit_map[i] = (i == 0) ? 1 : 0; // Block 0 used
    }
}

//method that makes sure the setup is working
void test_setup() {
    printf("Testing File System Setup...\n");

    // Print VCB details
    printf("Number of blocks: %d\n", vcb.num_of_blocks);
    printf("Block size: %d bytes\n", vcb.block_size);
    printf("Free block count: %d\n", vcb.free_count);

    // Add a test file entry
    strcpy(directory[0].file_name, "testfile");
    directory[0].start_block = 1;
    directory[0].file_size = 2;

    // Print directory test entry
    printf("Directory Entry:\n");
    printf("File: %s, Start Block: %d, Size: %d blocks\n",
           directory[0].file_name, directory[0].start_block, directory[0].file_size);

    // Print first 10 entries in bit map
    printf("Bit Map (first 10 blocks): ");
    for (int i = 0; i < 10; i++) {
        printf("%d ", vcb.bit_map[i]);
    }
    printf("\n");

    printf("Setup looks good!\n");
}


int main() {
    initialize_vcb();
    test_setup();

    return 0;
}

//int write() {

//}