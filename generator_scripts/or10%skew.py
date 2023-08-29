import pandas

def inputValsSkew(row):
    index=int(row.name)
    multi=index//20
    chunkIndex=index-multi*20
    timestamp=(multi)*1200000+(chunkIndex)
    vel=0.0
    qua=0.0
    #change here val to be half of total records
    if index>20000000:    
        if chunkIndex==1:
            vel=176.0
        if chunkIndex==10 or chunkIndex==18 or chunkIndex==5:
            qua=251.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty=empty.apply(inputValsSkew,axis=1)
empty.to_csv('or10%skew.csv', index=False,header=False) 