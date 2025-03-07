#include<pthread.h>
#include<stdio.h>


struct Vol_Control_Block{
    int num_of_blocks;
    int block_size;
    int free_count;
    //bit map - dont currently know what it is
};

//struct directory{
//    char file_name[20];
//};

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