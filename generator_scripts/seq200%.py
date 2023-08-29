import pandas

def inputVals2(row):
    index=int(row.name)
    multi=index//20
    chunkIndex=index-multi*20
    timestamp=(multi)*60000+(chunkIndex)
    vel=0.0
    if chunkIndex==0:
        vel=176.0
    qua=0.0
    if chunkIndex==10 or chunkIndex==18:
        qua=251.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty2=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty2=empty2.apply(inputVals2,axis=1)
empty2.to_csv('seq1200%.csv', index=False,header=False) 