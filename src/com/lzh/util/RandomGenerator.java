package com.lzh.util;

import java.util.Random;


public class RandomGenerator { 
	     
	    private static int next_pos;
         
	    //�������ظ��������
	     public static int generateNextShuffelPos(int current,int size){
	    	  Random random = new Random();
	    	  do{
	    		  next_pos = Math.abs(random.nextInt() % size);           //��������ܲ���������ȡ����ֵ
	    	  }while(next_pos == current);
	    	  
	    	  return next_pos;
	     }

}
