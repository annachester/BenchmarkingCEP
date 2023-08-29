import pandas

def inputValsSkew(row):
    index=int(row.name)
    multi=index//24
    chunkIndex=index-multi*24
    timestamp=(multi)*1200000+(chunkIndex)
    vel=0.0
    qua=0.0
    if index>20000000:
        if chunkIndex==1 or chunkIndex==3 or chunkIndex==8 or chunkIndex==12 or chunkIndex==17:
            vel=216.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty=empty.apply(inputValsSkew,axis=1)
empty.to_csv('iter210%skew.csv', index=False,header=False)