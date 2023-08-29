import pandas

def inputVals2(row):
    index=int(row.name)
    multi=index//15
    chunkIndex=index-multi*15
    timestamp=(multi)*1200000+(chunkIndex)
    vel=0.0
    if chunkIndex==1 or chunkIndex==2 or chunkIndex==4 or chunkIndex==6 or chunkIndex==7 or chunkIndex==8:
        vel=216.0
    qua=0.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty=empty.apply(inputVals2,axis=1)
empty.to_csv('iter2200%.csv', index=False,header=False)