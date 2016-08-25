# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2016
from streamsx.topology.schema import *

all_spl_types = StreamSchema("tuple<boolean b,"
                  "int8 i8, int16 i16, int32 i32, int64 i64,"
    		  "uint8 u8, uint16 u16, uint32 u32, uint64 u64,"
    		  "float32 f32, float64 f64,"
    		  "rstring r,"
    		  "complex32 c32,"
    		  "complex64 c64,"
    		  "list<rstring> lr,"
    		  "list<int32> li32,"
    		  "list<int64> li64,"
    		  "list<uint32> lui32,"
    		  "list<uint64> lui64,"
    		  "list<float32> lf32,"
    		  "list<float64> lf64,"
    		  "list<boolean> lb,"
    		  "map<int32,rstring> mi32r,"
    		  "map<rstring,uint32> mru32,"
    		  "map<rstring,int32> mri32,"
    		  "map<uint32,rstring> mu32r,"
    		  "map<int32,int32> mi32i32,"
    		  "map<uint32,uint32> mu32u32,"
    		  "map<rstring,rstring> mrr,"
    		  "map<float64,float64> mf64f64,"
    		  "map<float64,int32> mf64i32,"
    		  "map<float64,uint32> mf64u32,"
    		  "map<float64,rstring> mf64r,"
    		  "map<rstring,float64> mrf64,"
    		  "set<int32> si32>"
                  )

