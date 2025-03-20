#include<pthread.h>
#include<stdio.h>
#define TOTAL_BLOCKS 512

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