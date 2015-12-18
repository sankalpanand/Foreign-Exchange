__author__ = 'sankalp'

class MySQL:

    def insert(self, word, score):
        import MySQLdb
        conn = MySQLdb.connect(host= "localhost",
                          user="root",
                          passwd="",
                          db="big_data")
        x = conn.cursor()

        try:
           x.execute("""INSERT INTO TFIDF VALUES (%s,%s)""",(word, score))
           conn.commit()
        except:
           conn.rollback()

        conn.close()


    def select(self):
        pass
