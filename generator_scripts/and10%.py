import pandas

def inputVals(row):
    index=int(row.name)
    multi=index//20
    chunkIndex=index-multi*20
    timestamp=(multi)*1260000+(chunkIndex)
    vel=0.0
    if chunkIndex==1:
        vel=176.0
    qua=0.0
    if chunkIndex==10 or chunkIndex==18:
        qua=251.0
    ids="R"+"2000070"
    return pandas.Series([ids,"POINT (8.915096877268812 50.266944978521664)",timestamp,vel,qua,"F"])

empty=pandas.DataFrame(data=None,index=range(40000000),columns=range(6))
empty=empty.apply(inputVals,axis=1)
empty.to_csv('and10%.csv', index=False,header=False)