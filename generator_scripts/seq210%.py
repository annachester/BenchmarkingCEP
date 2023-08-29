import pandas

def inputVals2(row):
    index=int(row.name)
    multi=index//20
    chunkIndex=index-multi*20
    timestamp=(multi)*1260000+(chunkIndex)
    vel=0.0
    if chunkIndex==1:
        vel=176.0
    qua=300
    if chunkIndex==2:
        qua=249.0
    if chunkIndex==3:
        vel=179.0
    if chunkIndex==4 or chunkIndex==5:
        qua=240.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty=empty.apply(inputVals2,axis=1)
empty.to_csv('seq210%.csv', index=False,header=False)