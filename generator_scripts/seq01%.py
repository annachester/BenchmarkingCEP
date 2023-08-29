import pandas

def inputVals4(row):
    index=int(row.name)
    multi=index//20
    chunkIndex=index-multi*20
    timestamp=(multi)*1200000+(chunkIndex)
    vel=0.0
    qua=0.0
    if multi%100==0:    
        if chunkIndex==1:
            vel=176.0
        if chunkIndex==10 or chunkIndex==18:
            qua=251.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty=empty.apply(inputVals4,axis=1)
empty.to_csv('seq101%.csv', index=False,header=False) 